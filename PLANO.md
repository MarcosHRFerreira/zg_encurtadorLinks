# Plano do Projeto — Encurtador de URLs

## 1) Objetivo
Construir um encurtador de URLs com backend em Java (Spring Boot), frontend em typescript + Angular e persistência em PostgreSQL. O sistema deve ser stateless, escalável horizontalmente, com API REST pública para encurtar e redirecionar URLs e interface web simples.

- Backend: Java spring boot 21 - maven
- Frontend: typescript + Angular
- Banco de dados: PostgreSQL

📌 **Requisitos funcionais:**
1. O site deve permitir encurtar URLs originais, gerando um código único de até 6 caracteres.
   - Exemplo: www.google.com.br → www.corp.br/0kM7Z
   - A URL encurtada deve ser persistida no banco de dados e **não expirar**.
   - Submissões repetidas da mesma URL devem gerar códigos diferentes.

2. A aplicação deve ser **stateless**, **autoescalável** e suportar o máximo de acessos simultâneos.
   - SLA de 99%: no máximo 1 falha a cada 100 requisições.

3. Não deve haver autenticação.




4. Interface simples e funcional (ex: página branca com campo de texto e botão).

5. Disponibilizar uma **API REST** com:
   - Endpoint para encurtar URLs
   - Endpoint para redirecionar URLs encurtadas

6. Persistência dos dados deve ser clara e fácil de entender (sem criptografia).

7. O projeto deve conter **testes automatizados**.

8. Criar uma **documentação simples** explicando como executar o projeto.

🚀 **Entrega esperada:**
- Código em repositório público (ex: GitHub)
- README com instruções para execução local (ex: Docker, npm start)
- Deploy funcional em ambiente cloud (ex: Heroku ou Vercel)
- Arquivo com métricas de testes de performance (ex: JMeter ou Artillery)

Por favor, siga boas práticas de desenvolvimento e arquitetura. Se possível, use containers para facilitar o deploy e escalabilidade. Obrigado!


