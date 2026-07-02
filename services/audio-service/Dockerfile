# Stage 1
FROM golang:1.26.3-alpine AS build

WORKDIR /app

COPY go.mod go.sum ./

RUN go mod download

COPY . .

RUN go build -o server cmd/api/main.go


# Stage 2
FROM alpine:3.22.4

WORKDIR /app

COPY --from=build /app/server .

EXPOSE 8082

ENTRYPOINT [ "./server" ]
