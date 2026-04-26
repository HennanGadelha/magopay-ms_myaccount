package com.magopay.my_account.core.domain;

import com.magopay.my_account.core.domain.exception.DomainException;
import com.magopay.my_account.core.domain.exception.InvalidDocumentException;
import com.magopay.my_account.core.domain.exception.InvalidEmailException;
import com.magopay.my_account.core.domain.exception.InvalidPasswordException;
import com.magopay.my_account.core.domain.exception.InvalidUserNameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User domain entity")
class UserTest {

    private static final UUID VALID_ID    = UUID.randomUUID();
    private static final String VALID_NAME = "João Silva";
    private static final String VALID_EMAIL = "joao@exemplo.com";
    private static final String VALID_DOC  = "12345678901";
    private static final String VALID_PASS = "hashed_password";

    // ─────────────────────────────────────────────────────────────────
    // create()
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("create() factory")
    class CreateFactory {

        @Test
        @DisplayName("deve criar usuario com status IN_ANALYZING por padrao")
        void shouldCreateWithInAnalyzingStatus() {
            User user = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS);

            assertThat(user.getId()).isEqualTo(VALID_ID);
            assertThat(user.getName()).isEqualTo(VALID_NAME);
            assertThat(user.getEmail()).isEqualTo(VALID_EMAIL);
            assertThat(user.getDocument()).isEqualTo(VALID_DOC);
            assertThat(user.getPassword()).isEqualTo(VALID_PASS);
            assertThat(user.getStatus()).isEqualTo(UserStatus.IN_ANALYZING);
        }

        @Test
        @DisplayName("deve lancar DomainException quando id for nulo")
        void shouldThrowWhenIdIsNull() {
            assertThatThrownBy(() -> User.create(null, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("id cannot be null");
        }

        @Test
        @DisplayName("deve lancar excecao quando name for nulo")
        void shouldThrowWhenNameIsNull() {
            assertThatThrownBy(() -> User.create(VALID_ID, null, VALID_EMAIL, VALID_DOC, VALID_PASS))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("deve lancar InvalidUserNameException quando name for vazio apos trim")
        void shouldThrowWhenNameIsBlank() {
            assertThatThrownBy(() -> User.create(VALID_ID, "   ", VALID_EMAIL, VALID_DOC, VALID_PASS))
                    .isInstanceOf(InvalidUserNameException.class);
        }

        @Test
        @DisplayName("deve fazer trim do name")
        void shouldTrimName() {
            User user = User.create(VALID_ID, "  João  ", VALID_EMAIL, VALID_DOC, VALID_PASS);
            assertThat(user.getName()).isEqualTo("João");
        }

        @Test
        @DisplayName("deve lancar excecao quando email for nulo")
        void shouldThrowWhenEmailIsNull() {
            assertThatThrownBy(() -> User.create(VALID_ID, VALID_NAME, null, VALID_DOC, VALID_PASS))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("deve lancar InvalidEmailException quando email for vazio apos trim")
        void shouldThrowWhenEmailIsBlank() {
            assertThatThrownBy(() -> User.create(VALID_ID, VALID_NAME, "   ", VALID_DOC, VALID_PASS))
                    .isInstanceOf(InvalidEmailException.class);
        }

        @Test
        @DisplayName("deve lancar InvalidEmailException quando formato do email for invalido")
        void shouldThrowWhenEmailFormatInvalid() {
            assertThatThrownBy(() -> User.create(VALID_ID, VALID_NAME, "nao-e-email", VALID_DOC, VALID_PASS))
                    .isInstanceOf(InvalidEmailException.class)
                    .hasMessageContaining("invalid");
        }

        @Test
        @DisplayName("deve normalizar email para lowercase e trim")
        void shouldNormalizeEmail() {
            User user = User.create(VALID_ID, VALID_NAME, "  JOAO@EXEMPLO.COM  ", VALID_DOC, VALID_PASS);
            assertThat(user.getEmail()).isEqualTo("joao@exemplo.com");
        }

        @Test
        @DisplayName("deve lancar excecao quando document for nulo")
        void shouldThrowWhenDocumentIsNull() {
            assertThatThrownBy(() -> User.create(VALID_ID, VALID_NAME, VALID_EMAIL, null, VALID_PASS))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("deve lancar InvalidDocumentException quando document for vazio apos normalizacao")
        void shouldThrowWhenDocumentIsBlank() {
            assertThatThrownBy(() -> User.create(VALID_ID, VALID_NAME, VALID_EMAIL, "   ", VALID_PASS))
                    .isInstanceOf(InvalidDocumentException.class);
        }

        @Test
        @DisplayName("deve normalizar CPF removendo pontos e traco")
        void shouldNormalizeCpf() {
            User user = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, "123.456.789-01", VALID_PASS);
            assertThat(user.getDocument()).isEqualTo("12345678901");
        }

        @Test
        @DisplayName("deve lancar excecao quando password for nulo")
        void shouldThrowWhenPasswordIsNull() {
            assertThatThrownBy(() -> User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, null))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("deve lancar InvalidPasswordException quando password for vazio apos trim")
        void shouldThrowWhenPasswordIsBlank() {
            assertThatThrownBy(() -> User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, "   "))
                    .isInstanceOf(InvalidPasswordException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // reconstitute()
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("reconstitute() factory")
    class Reconstitute {

        @Test
        @DisplayName("deve reconstituir usuario com status informado")
        void shouldReconstituteWithGivenStatus() {
            User user = User.reconstitute(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS, UserStatus.ACTIVE);
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("deve lancar DomainException quando status for nulo")
        void shouldThrowWhenStatusIsNull() {
            assertThatThrownBy(() ->
                    User.reconstitute(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS, null))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("status cannot be null");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Status mutation methods
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("metodos de mutacao de status")
    class StatusMutation {

        @Test
        @DisplayName("activate() deve mudar status para ACTIVE")
        void shouldActivate() {
            User user = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS);
            user.activate();
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("deactivate() deve mudar status para INACTIVE")
        void shouldDeactivate() {
            User user = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS);
            user.deactivate();
            assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        }

        @Test
        @DisplayName("startAnalyzing() deve mudar status para IN_ANALYZING")
        void shouldStartAnalyzing() {
            User user = User.reconstitute(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS, UserStatus.ACTIVE);
            user.startAnalyzing();
            assertThat(user.getStatus()).isEqualTo(UserStatus.IN_ANALYZING);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Data mutation methods
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("metodos de mutacao de dados")
    class DataMutation {

        @Test
        @DisplayName("changeName() deve atualizar name")
        void shouldChangeName() {
            User user = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS);
            user.changeName("Novo Nome");
            assertThat(user.getName()).isEqualTo("Novo Nome");
        }

        @Test
        @DisplayName("changeName() deve lancar excecao para nome invalido")
        void shouldThrowOnInvalidNewName() {
            User user = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS);
            assertThatThrownBy(() -> user.changeName("  "))
                    .isInstanceOf(InvalidUserNameException.class);
        }

        @Test
        @DisplayName("changeEmail() deve atualizar email normalizado")
        void shouldChangeEmail() {
            User user = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS);
            user.changeEmail("NOVO@EXEMPLO.COM");
            assertThat(user.getEmail()).isEqualTo("novo@exemplo.com");
        }

        @Test
        @DisplayName("changeEmail() deve lancar excecao para formato invalido")
        void shouldThrowOnInvalidNewEmail() {
            User user = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS);
            assertThatThrownBy(() -> user.changeEmail("invalido"))
                    .isInstanceOf(InvalidEmailException.class);
        }

        @Test
        @DisplayName("changeDocument() deve normalizar CPF")
        void shouldChangeAndNormalizeDocument() {
            User user = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS);
            user.changeDocument("987.654.321-00");
            assertThat(user.getDocument()).isEqualTo("98765432100");
        }

        @Test
        @DisplayName("changeDocument() deve lancar excecao para documento invalido")
        void shouldThrowOnInvalidNewDocument() {
            User user = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS);
            assertThatThrownBy(() -> user.changeDocument("  "))
                    .isInstanceOf(InvalidDocumentException.class);
        }

        @Test
        @DisplayName("changePassword() deve atualizar senha")
        void shouldChangePassword() {
            User user = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS);
            user.changePassword("new_hash");
            assertThat(user.getPassword()).isEqualTo("new_hash");
        }

        @Test
        @DisplayName("changePassword() deve lancar excecao para senha invalida")
        void shouldThrowOnInvalidNewPassword() {
            User user = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS);
            assertThatThrownBy(() -> user.changePassword("  "))
                    .isInstanceOf(InvalidPasswordException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // equals / hashCode / toString
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("equals, hashCode e toString")
    class EqualsHashCodeToString {

        @Test
        @DisplayName("equals deve ser true para mesmo objeto")
        void equalsSameInstance() {
            User user = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS);
            assertThat(user).isEqualTo(user);
        }

        @Test
        @DisplayName("equals deve ser true para usuarios com mesmo id")
        void equalsById() {
            User a = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS);
            User b = User.reconstitute(VALID_ID, "Outro", "outro@ex.com", "99988877766", "hash2", UserStatus.ACTIVE);
            assertThat(a).isEqualTo(b);
        }

        @Test
        @DisplayName("equals deve ser false para usuarios com ids diferentes")
        void notEqualsDifferentId() {
            User a = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS);
            User b = User.create(UUID.randomUUID(), VALID_NAME, "outro@ex.com", "99988877766", VALID_PASS);
            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("equals deve ser false para nulo")
        void notEqualsNull() {
            User user = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS);
            assertThat(user).isNotEqualTo(null);
        }

        @Test
        @DisplayName("equals deve ser false para tipo diferente")
        void notEqualsDifferentType() {
            User user = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS);
            assertThat(user).isNotEqualTo("uma string qualquer");
        }

        @Test
        @DisplayName("hashCode deve ser igual para usuarios com mesmo id")
        void hashCodeById() {
            User a = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS);
            User b = User.reconstitute(VALID_ID, "Outro", "outro@ex.com", "99988877766", "hash2", UserStatus.ACTIVE);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("toString nao deve conter password")
        void toStringShouldNotExposePassword() {
            User user = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS);
            assertThat(user.toString()).doesNotContain(VALID_PASS);
        }

        @Test
        @DisplayName("toString deve conter id e email")
        void toStringShouldContainIdAndEmail() {
            User user = User.create(VALID_ID, VALID_NAME, VALID_EMAIL, VALID_DOC, VALID_PASS);
            assertThat(user.toString())
                    .contains(VALID_ID.toString())
                    .contains(VALID_EMAIL);
        }
    }
}

