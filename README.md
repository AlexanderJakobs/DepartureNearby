
# HVV Departure System

## 📋 Projektbeschreibung

Dieses Projekt wurde im Rahmen eines Hochschulprojekts mit drei weiteren Kommilitonen entwickelt. Ziel des Projekts war die Entwicklung eines verteilten Systems zur Abfrage von Echtzeitabfahrtsinformationen im HVV.

Das System ermöglicht es Benutzern, durch einfache Eingabe einer Adresse die nächstgelegenen Haltestellen und deren aktuelle Abfahrtszeiten zu erhalten.

---

## 🏗️ Systemarchitektur

Das System besteht aus **drei Hauptkomponenten**:

1. **DisplayManager** - Orchestriert Anfragen und koordiniert die Kommunikation zwischen Komponenten, nimmt die Anfragen vom User an und zeigt den Abfahrtplan am Ende
2. **Locationhandler** - Wandelt Adressen in geografische Koordinaten um
3. **Transportplan** - Ruft Abfahrtsinformationen basierend auf Koordinaten ab

Die Kommunikation erfolgt über **zwei RPC-Ebenen**:

### Internal RPC (gRPC)
- Asynchrone Kommunikation zwischen internen Komponenten
- Protocol Buffers für typsichere Nachrichtenserialisierung
- Message-basierte Architektur mit Correlation Tracking

### External REST
- HTTP/REST-API für externe Clients
- Integration externer Dienste über REST

---

## 🌐 Externe Services

Das System integriert zwei externe Services über APIs:

### 1. Nominatim (OpenStreetMap)
- **Zweck**: Geocodierung von Adressen
- **Funktion**: Konvertiert Textadressen in geografische Koordinaten (Latitude/Longitude)

### 2. Geofox (HVV)
- **Zweck**: Abfrage von Echtzeitfahrplandaten
- **Funktion**: Liefert aktuelle Abfahrtszeiten, Verspätungen und Haltestelleninformationen

---

## ⚙️ Load Balancing & Skalierung

Die Lastverteilung und Skalierbarkeit wird durch **Kubernetes** gewährleistet:

- Horizontale Skalierung einzelner Komponenten
- Automatische Load-Distribution
- Container-Orchestrierung für hohe Verfügbarkeit
- Health Checks und Self-Healing

---

## 🛠️ Technologie-Stack

- **Java 17** - Programmiersprache
- **Spring Boot 3.2** - Application Framework
- **gRPC** - Internal RPC Framework
- **Protocol Buffers** - Nachrichtenserialisierung
- **Docker** - Containerisierung
- **Kubernetes** - Orchestrierung und Load Balancing
- **Gradle** - Build-Tool

---

## 📚 Dokumentation

Die detaillierte technische Dokumentation finden Sie in:

-  [Arc42 Dokumentation](docs/arc42/doku.md)
---

## 🚀 Quick Start

### Voraussetzungen

- Java 17+
- Docker & Docker Compose
- Geofox API Key



## 👥 Team

Raffael Wolpers, Maja Trucks, Ibrahim Agboola und Alexander Jakobs.

---

## 📄 Lizenz

Dieses Projekt wurde für akademische Zwecke entwickelt.

