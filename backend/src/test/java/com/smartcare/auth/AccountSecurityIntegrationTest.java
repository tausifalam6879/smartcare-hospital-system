package com.smartcare.auth;
import com.smartcare.auth.service.*;
import com.smartcare.auth.security.AuthAttemptLimiter;
import com.smartcare.auth.repository.UserAccountRepository;
import com.smartcare.auth.web.*;
import com.smartcare.common.error.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @Transactional
class AccountSecurityIntegrationTest {
    @Autowired AuthService auth;
    @Autowired AccountProfileService profiles;
    @Autowired AccountRecoveryService recovery;
    @Autowired UserAccountRepository users;
    @Autowired JwtDecoder decoder;
    private AuthResponse patient(String suffix) { return auth.register(new RegisterRequest("+9195111100"+suffix,null,"Private profile", "test-password-strong",null,null,null,null,"en")); }
    @Test void passwordAndLogoutRevokePreviouslyValidJwt() {
        var account=patient("01");
        assertThat(decoder.decode(account.accessToken()).getSubject()).isEqualTo(account.user().id().toString());
        assertThatThrownBy(() -> profiles.changePassword(account.user().id(),"incorrect", "new-password-strong")).isInstanceOf(AccessDeniedException.class);
        profiles.changePassword(account.user().id(),"test-password-strong","new-password-strong"); users.flush();
        assertThatThrownBy(() -> decoder.decode(account.accessToken())).isInstanceOf(JwtValidationException.class);
        var next=auth.login(new LoginRequest(account.user().mobileNumber(),"new-password-strong"));
        decoder.decode(next.accessToken());
        profiles.revoke(account.user().id()); users.flush();
        assertThatThrownBy(() -> decoder.decode(next.accessToken())).isInstanceOf(JwtValidationException.class);
    }
    @Test void privatePhotoIsValidatedReencodedAndRemovable() throws Exception {
        var owner=patient("02"); var other=patient("03");
        assertThatThrownBy(() -> profiles.photo(owner.user().id(),"<svg>bad</svg>".getBytes())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profiles.photo(owner.user().id(),new byte[1_048_577])).isInstanceOf(IllegalArgumentException.class);
        var bytes=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(25,25,BufferedImage.TYPE_INT_RGB),"png",bytes);
        profiles.photo(owner.user().id(),bytes.toByteArray());
        assertThat(profiles.get(owner.user().id()).photo()).startsWith("data:image/png;base64,");
        assertThat(profiles.get(other.user().id()).photo()).isNull();
        profiles.update(owner.user().id(),"Updated name","FEMALE");
        assertThat(profiles.get(owner.user().id()).gender()).isEqualTo("FEMALE");
        profiles.deletePhoto(owner.user().id()); assertThat(profiles.get(owner.user().id()).photo()).isNull();
    }
    @Test @WithMockUser(roles="SUPER_ADMIN") void recoveryIsMobileBoundSingleUseAndRevokesSessions() {
        var owner=patient("04"); var issued=recovery.issue(owner.user().id());
        assertThatThrownBy(() -> recovery.recover("+919511110099",issued.code(),"recovered-password")).isInstanceOf(AccessDeniedException.class);
        recovery.recover(owner.user().mobileNumber(),issued.code(),"recovered-password"); users.flush();
        assertThatThrownBy(() -> recovery.recover(owner.user().mobileNumber(),issued.code(),"recovered-password")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> decoder.decode(owner.accessToken())).isInstanceOf(JwtValidationException.class);
        assertThat(auth.login(new LoginRequest(owner.user().mobileNumber(),"recovered-password")).user().id()).isEqualTo(owner.user().id());
    }
    @Test void limiterRejectsRepeatedAttemptsAndAllowsOtherAccounts() {
        var limiter=new AuthAttemptLimiter(Clock.fixed(Instant.parse("2026-09-28T00:00:00Z"),ZoneOffset.UTC));
        for(int i=0;i<10;i++) limiter.check("test");
        assertThatThrownBy(() -> limiter.check("test")).isInstanceOf(TooManyRequestsException.class);
        limiter.check("other"); limiter.clear("test"); limiter.check("test");
    }
}
