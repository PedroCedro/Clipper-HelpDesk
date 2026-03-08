# Debito Tecnico

## Pendencias de ambiente

- Instalar Node.js no Windows para disponibilizar `node` e `npm` no `PATH`
- Validar os comandos `node -v` e `npm -v` apos a instalacao
- Instalar Maven no Windows ou adicionar `mvn` ao `PATH`
- Validar o comando `mvn -v` apos a configuracao

## Pendencias de execucao

- Executar o frontend com:

```powershell
cd frontend
npm install
npm run dev
```

- Executar o backend com:

```powershell
cd backend
mvn spring-boot:run
```

## Observacoes

- O backend foi ajustado para subir sem dependencia imediata de PostgreSQL na estrutura inicial
- A validacao completa da execucao ficou pendente por falta de `npm` e `mvn` no ambiente
