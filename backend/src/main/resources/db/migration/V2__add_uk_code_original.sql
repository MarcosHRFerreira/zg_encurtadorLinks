-- Adiciona constraint única composta para (code, original_url)
ALTER TABLE short_urls
    ADD CONSTRAINT uk_code_original UNIQUE (code, original_url);