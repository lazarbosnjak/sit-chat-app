# SitApp

SitApp je veb aplikacija za razmenu poruka, multimedijalnog sadržaja i grupnu komunikaciju između korisnika. Projekat je rađen za predmete SVT i KVT.

## Tehnologije

- Backend: Spring Boot, Spring Security, Spring Data JPA, WebSocket/STOMP
- Multimedijalni mikroservis: Go
- Frontend: Angular, TypeScript, pnpm
- Baza podataka: PostgreSQL
- Infrastrktura: Docker Compose

## Struktura projekta

- `backend/` - Spring Boot API aplikacija
- `frontend/` - Angular klijentska aplikacija
- `services/` - dodatni servisi
- `docker-compose.yml` - lokalna infrastruktura

## Pokretanje

Backend:

```bash
docker compose up --build
```

Frontend:

```bash
cd frontend
pnpm install
pnpm start
```

## Napomene

Pre pokretanja proveriti podešavanja baze i ostalih servisa u konfiguracionim fajlovima. Tajne podatke i lokalne kredencijale ne treba čuvati u repozitorijumu.
