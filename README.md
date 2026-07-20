# SitChatApp

SitApp je veb aplikacija za razmenu poruka u realnom vremenu, multimedijalnog sadržaja i grupnu komunikaciju između korisnika.

Projekat je rađen za predmete **Klijentske veb tehnologije** i **Serverske veb tehnologije** sa II godine smera "Softverske i informacione tehnologije" na Fakultetu tehničkih nauka na Univerzitetu u Novom Sadu.

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

Pre pokretanja kopirati `.env.example` fajlove u odgovarajuce `.env` fajlove i popuniti lokalne vrednosti:

```bash
cp backend/.env.example backend/.env
cp services/audio-service/.env.example services/audio-service/.env
```

Ako `.env` fajlove cuvate na drugim putanjama, promeniti `env_file` putanje u `docker-compose.yml`.

Backend:

```bash
docker compose up --build
```

Frontend:

> Napomena: potrebno je imati **Node.js v22+**

```bash
cd frontend
pnpm install
pnpm start
```

## Demo

Možete pronaći demo na stranici: [https://kvt.lazarb.dev](https://kvt.lazarb.dev)

| Username | Password |
| -------- | -------- |
| `admin`  | `admin123`  |
| `luka`    | `password123`    |
| `jovana`    | `password123`    |
