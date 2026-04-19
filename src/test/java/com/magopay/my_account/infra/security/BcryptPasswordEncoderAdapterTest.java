package com.magopay.my_account.infra.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BcryptPasswordEncoderAdapter")
class BcryptPasswordEncoderAdapterTest {

    private final BcryptPasswordEncoderAdapter adapter = new BcryptPasswordEncoderAdapter();

    @Test
    @DisplayName("encode deve retornar hash nao nulo e nao vazio")
    void shouldReturnNonNullHash() {
        String hash = adapter.encode("minhasenha");
        assertThat(hash).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("encode deve retornar valor diferente da senha original")
    void shouldReturnDifferentFromInput() {
        String raw = "minhasenha";
        String hash = adapter.encode(raw);
        assertThat(hash).isNotEqualTo(raw);
    }

    @Test
    @DisplayName("encode deve gerar hashes diferentes para mesma senha (salt aleatorio)")
    void shouldGenerateDifferentHashesForSameInput() {
        String hash1 = adapter.encode("minhasenha");
        String hash2 = adapter.encode("minhasenha");
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("encode deve iniciar com prefixo BCrypt")
    void shouldStartWithBcryptPrefix() {
        String hash = adapter.encode("minhasenha");
        assertThat(hash).startsWith("$2a$");
    }
}

