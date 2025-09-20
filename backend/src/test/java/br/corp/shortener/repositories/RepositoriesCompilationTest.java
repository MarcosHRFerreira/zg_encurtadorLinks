package br.corp.shortener.repositories;

import br.corp.shortener.entities.ShortUrl;
import br.corp.shortener.entities.ShortUrlAccess;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RepositoriesCompilationTest {

    // Este teste não executa JPA; serve apenas para garantir que as interfaces
    // existam no classpath e possam ser referenciadas em cenários de mock.
    @Test
    void compileTimeCheck() {
        assertNotNull(ShortUrl.class);
        assertNotNull(ShortUrlAccess.class);
        assertNotNull(ShortUrlRepository.class);
        assertNotNull(ShortUrlAccessRepository.class);
    }
}