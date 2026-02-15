# Dokumentation vom Projekt<br>"Abfahrten in der Nähe"
##### Raffael Wolpers, Ibrahim Agboola, Alexander Jakobs, Maja Trucks

## 1. Introduction & Goals
- Location is either entered by the user or automatically retrieved from GPS
- 3 stations closest to the starting position
- Stations sorted by distance in ascending order
- The next departure point in each direction is displayed for each station 
- Time is displayed in minutes  
- Application updates after pressing a refresh button 
- Internet connection required 
- Response should be received within 5 seconds;<br>otherwise, an error message "no internet connection" will be displayed 
- Loading indicator (of any type) indicating that the search is in progress,<br> status update for user 

### Quality goals:
| **Quality category**    | **Quality**         | **Description**                                                                                                                         |
|:------------------------|:--------------------|:----------------------------------------------------------------------------------------------------------------------------------------|
| Functional stability 		 | Basic functionality | System will always search and show the 3 closest stations<br> to a user’s starting point                                                |
| Performance				         | Stability			        | 15 Minutes during Labs -> must function at least for 15 minutes strait                                                                  |
| Cultural and Regional		 | Multilanguage		     | The user interface texts must be translatable into English<br> or German using a translation file that supports the ASCII character set |

### Transparencies
| **Transparency Type** | **Relevance**                                                                              |
|:----------------------|:-------------------------------------------------------------------------------------------|
| Acces                 | The user must be in the HVV region -> Show the user that the data is just for HVV-region   |
| Error                 | The user should not notice that there is an error. -> Display time of the last data update |

### Stakeholder

| Role           | Name                                                                            | Expectations |
|:---------------|:--------------------------------------------------------------------------------|:-------|
| Developer      | Jakobs Alexander, Agboola Ibrahim<br>Ziegler Luca, Wolpers Raffael, Trucks Maja | • documentation matches code<br>• Java<br>• runnable from IDE<br>• English code |
| Project Owner  | Becke, Martin                                                                   | • An API connection with an external service (Extern GraphQL, RESTful)<br>and ICC (Intern RPC)<br>• Loadsharing<br>• Support a service orchestration with RPC<br>• measurable goals |

## Use Case: Show Departures from 3 Nearby Stations

### Description
A person wants to know the nearest upcoming departures from an address in Hamburg that is passed to the app.

### Primary Actor
Citizan
### Goal
View the three nearest public transport stations and their next departures.

### Trigger
The user provides an address to the system in the format: street name + house number.

### Pre-conditions
- The address must be located in Hamburg
- The device must be connected to the internet

### Post-conditions
The user receives a schedule showing:
- Three nearest stations
- Departures from each station
- All directions
- All available public transport types

### Basic Flow
1. The user enters an address into the system.
2. The system displays a table of departures containing:
  - Line name
  - Departure time
  - Stop name


## 2. Constraints
- Data is only relevant for Hamburg, Germany, and dependent <br>on the HVV (Hamburg public transport system)
- API connection to an external service (external GraphQL, RESTful)<br> and the ICC (internal RPC)
- Load sharing is supported
- Service orchestration via RPC is supported
- Geofox Thin interface, HTTP, JSON
- 1 request/second to HVV and Nominatim
- stations showed only in radius of 3 km

## 3. Context & Scope
### Kontextsicht

| Element             | Description                                                                                                                  |     |
| :------------------ | :--------------------------------------------------------------------------------------------------------------------------- | --- |
| User                | Gives his position (Streetname and number)                                                                                   |     |
| hvv                 | Station data provider                                                                                                        |     |
| Locationconverter   | An external provider, which handles the positioning for us. <br>It should provide us with coordinates/position for the user. |     |
| "Nearby Departures" | Our System                                                                                                                   |     |

### Technical context 
![Techsicht](/docs/arc42/images/TechnicalContext.png)

| Node          | Communication   | Data format | Description                                              |
|---------------|-----------------|-------------|----------------------------------------------------------|
| JRE           | JDK 21          | --          | Runs the code of our system                              |
| Geofox API    | REST over HTTPS | JSON        | Exchanges coordinated and departure plan with our system |
| Nominatim API | REST over HTTPS | JSON        | Returns us coordinates                                   |
| GitLab        | HTTPS           | --          | Version control, integration kubernetes                  |
| Gradle        | --              | --          | Build tool                                               |
| Spring Boot   | --              | --          | Framework to build application                           |

## 4. Solution strategy

| Name                                                                                                              | Precondition                                                             | Postcondition                                                                        | Parameter                                                                       | Description                                                                                                             |
| ----------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| External REST request `GET /api/location`                                                                         | App is started and waiting for requests                                  | Request accepted by internal orchestration; client receives HTTP 200/400/500         | address: String                                                                 | Public entry point. Passes the address entered by the user to the internal RPC workflow (ExternalRest → DisplayManager) |
| DisplayManagerIngress.userPassLocation() <br/> (ExternalRest → DisplayManager, gRPC)                              | ExternalInput.address is set                                             | DisplayManager acknowledges acceptance via ACK; processing continues asynchronously  | address: String                                                                 | Passes the address to the DisplayManager via gRPC (asynchronous ACK pattern)                                            |
| LocationHandlerIngress.userPassLocation() <br/> (DisplayManager → LocationHandler, gRPC)                          | DisplayManager has validated/parsed the address                          | LocationHandler acknowledges acceptance via ACK; starts geocoding asynchronously     | meta: RequestMeta, address: Address                                             | DisplayManager passes the structured address to the LocationHandler and a RequestID is created                          |
| GeocodingService.geocode() <br/>(LocationHandler → ExternalRest, gRPC wrapper for Nominatim)                      | Address is present (Street + HouseNumber)                               | Coordinates are returned                                                             | meta: RequestMeta, address: Address                                             | ExternalRest encapsulates Nominatim. LocationHandler receives coordinates for the address                               |
| TransportPlanIngress.getDepartures() <br/> (LocationHandler → TransportPlan, gRPC)                                | LocationHandler has valid coordinates                                    | TransportPlan acknowledges acceptance via ACK; starts departure query asynchronously | meta: RequestMeta, coordinates: Coordinates                                     | Internal RPC step: passing the coordinates to the TransportPlan                                                         |
| GeofoxClient.findNearbyStations() <br/> (TransportPlan → ExternalRest, gRPC wrapper for Geofox/HVV)               | Coordinates are present                                                  | Returns DepartureStationList plus ResultMeta                                         | meta: RequestMeta, coordinates: Coordinates                                     | ExternalRest encapsulates Geofox/HVV. TransportPlan receives the 3 nearest stations                                     |
| GeofoxClient.fetchDeparturesForMasterStations() <br/> (TransportPlan → ExternalRest, gRPC wrapper for Geofox/HVV) | Station ID or station name is known                                      | Returns departures plus ResultMeta                                                   | meta: RequestMeta, station_id: String or station_name: String                   | Fetch departures for a specific station                                                                                 |
| DisplayManagerIngress.showDepartures() <br/>(TransportPlan → DisplayManager, gRPC)                                | TransportPlan has generated a DepartureStation list                      | DisplayManager acknowledges acceptance via ACK; UI/display is updated                | meta: RequestMeta, stations: List<DepartureStation>, data_fetched_at: Timestamp | Result return channel: TransportPlan sends stations + departures (and fetch timestamp) to the DisplayManager            |
| External REST request `GET /api/status`                                                                           | At least one Geofox/HVV call has been executed (otherwise “no data yet”) | Last supplier contact is returned as a string                                        | —                                                                               | Status/transparency endpoint: returns the timestamp of the last successful data supplier interaction                    |
| External REST request `GET /api/health`                                                                           | —                                                                        | HTTP 200 OK                                                                          | —                                                                               | Simple health check for deployment/Compose/Kubernetes                                                                   |


Address:

-- streetName: String (Mönckebergstraße)

-- houseNumber: String (7)

-- city: String (Hamburg)

-- postalCode: String (20095)

-- country: String (Germany)

DepartureStation:

-- stationName: String (Dammtor)

-- futureDepartures: List<Departure>

Departure:

-- LineName: String (z.B. 5, Elbgaustrasse)

-- DepatureTime: String

Coordinates:

-- X-Coordinate: String

-- Y-Coordinate: String
### Technical decisions
**Implementation in Java**, because nothing stands against it and the developer team prefer the language

**GitLab**, simplifies integration with ICC

**Kanban**, for a good overview of the progress and for the documentation so it is all in one place. It is recommended by the German Innenministerium (Orgahandbuch - Kanban).

**Geolocation**, [Nominatim](https://nominatim.org/) -> openstreetmap -> 1 free use per second


## 5. Building block view
### Application
![Bausteinsicht](/docs/arc42/images/Baustein_v3.1.png)

## 6. Runtime view
### Sequence diagrams:

#### Display Manager
![SequenzDM](/docs/arc42/images/SeqDisplayManager.drawio.png)

#### Locationhandler
![SequenzLh](/docs/arc42/images/SeqLocationhandler.drawio.png)

#### Transportplan
![SequenzLh](/docs/arc42/images/SeqTransportplan.drawio.png)

### Technical view (middleware)
The following part describes a simplified, implementation-oriented middleware architecture. The architecture separates internal asynchronous communication from external REST communication.

#### Overview
The middleware consists of two clearly separated parts:

**Internal RPC Middleware** – mediates exclusively between internal components. gRPC is intended to be used as the technology.

**External REST Middleware** – encapsulates all external interfaces (clients and APIs).

The separation is strict. Internal components are not aware of external APIs or any HTTP-related details.

#### Internal RPC Middleware (Asynchronous)
**Purpose**
The Internal RPC Middleware exclusively mediates between the following three internal components:

- DisplayManager
    
- LocationHandler
    
- TransportPlan
    

It provides asynchronous RPC communication. All calls are non-blocking and message-based.

External systems (e.g. Nominatim, hvv/Geofox) are not part of this middleware.

**Communication Principles**
- Communication style: asynchronous RPC
    
- No synchronous responses
    
- Results are delivered via return channels or events
    
- Each message contains a `correlationId`
**Involved Interfaces (Minimal)**
- **DisplayManager → LocationHandler**
    
    - `ResolveLocationRequested`
        
- **LocationHandler → TransportPlan**
    
    - `GetDeparturesRequested`
        
- **TransportPlan → DisplayManager**
    
    - `DeparturesResolved`
**Stub Structure**
For each internal service, two technical components exist:

- **Caller Stub**
    
    - creates and sends requests
        
- **Invoked Stub**
    
    - receives requests and invokes the local implementation
        

The stubs are purely technical and contain no business logic.
#### External REST Middleware

**Purpose**

The External REST Middleware encapsulates all external interfaces:

- Public RESTful API for the product
    
- Access to external APIs
    

It is the only layer that uses HTTP/REST.

**External APIs**

- **Nominatim API** (address-to-coordinate resolution)
    
- **hvv / Geofox API** (departure data)
    

Access is performed via dedicated API clients:

- `NominatimApiClient`
    
- `GeofoxApiClient`
    

Internal components never access these APIs directly.

#### Interaction with the Internal Middleware

After a REST request:

- The REST middleware validates the request
    
- Starts internal processing via the Internal RPC Middleware
    
- Aggregates results
    
- Returns an HTTP response
    

#### Summary

- Internal communication: asynchronous RPC
    
- Involved internal components: DisplayManager, LocationHandler, TransportPlan
    
- No external dependencies within the internal middleware
    
- REST and external APIs are used exclusively in the External REST Middleware

- External Access and Load Balancing with Kubernetes

![Loadbalancer](/docs/arc42/images/Loadbalancer.jpeg)

External Client: https://nearbydepartures.informatik.haw-hamburg.de/api/location?address=...

Ingress: Handles TLS (HTTPS) and routes requests to the externalrest service.

Service: externalrest Service load balances requests across all externalrest pods (2 replicas).

Internal Services: displaymanager, locationhandler, and transportplan are accessed via ClusterIP Services and also load balance requests among their respective pods.


Clients only see the domain; NodePort or cluster IP is not exposed externally.

Ingress handles TLS (HTTPS) and routes traffic to the service.

The Service load balances traffic across pods of externalrest.

Internal services also load balance traffic among replicas.

This setup ensures high availability, fault tolerance, and even distribution of requests across pods and services.