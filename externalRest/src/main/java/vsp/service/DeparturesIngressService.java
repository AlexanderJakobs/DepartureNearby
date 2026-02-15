package vsp.service;

import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vsp.*;
import vsp.client.GeofoxClient;

import java.time.Instant;
import java.util.List;

/**
 * gRPC Service Implementation für Departures (Geofox API).
 * Wird von transportplan aufgerufen.
 */
@GrpcService
public class DeparturesIngressService extends DeparturesServiceGrpc.DeparturesServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(DeparturesIngressService.class);

    private final GeofoxClient geofoxClient;

    public DeparturesIngressService(GeofoxClient geofoxClient) {
        this.geofoxClient = geofoxClient;
        log.info("DeparturesServiceImpl initialized");
    }

    @Override
    public void getNearbyStations(GetNearbyStationsRequest request,
                                  StreamObserver<GetNearbyStationsResponse> responseObserver) {
        String correlationId = request.hasMeta() ? request.getMeta().getCorrelationId() : "unknown";
        int maxStations = request.getMaxStations() > 0 ? request.getMaxStations() : 10;

        log.info("[{}] GetNearbyStations request for coordinates: lat={}, lon={}",
                correlationId,
                request.getCoordinates().getLatitude(),
                request.getCoordinates().getLongitude());

        try {
            List<DepartureStation> stations = geofoxClient.findNearbyStations(
                    request.getCoordinates(), maxStations);

            GetNearbyStationsResponse response = GetNearbyStationsResponse.newBuilder()
                    .setStations(DepartureStationList.newBuilder()
                            .addAllStations(stations)
                            .build())
                    .setResultMeta(ResultMeta.newBuilder()
                            .setGeneratedAt(Timestamp.newBuilder()
                                    .setSeconds(Instant.now().getEpochSecond())
                                    .build())
                            .setSource("Geofox/HVV")
                            .build())
                    .build();

            log.info("[{}] GetNearbyStations success: {} stations found",
                    correlationId, stations.size());

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (GeofoxClient.GeofoxApiException e) {
            log.warn("[{}] Geofox API error: {}", correlationId, e.getMessage());

            GetNearbyStationsResponse errorResponse = GetNearbyStationsResponse.newBuilder()
                    .setError(ErrorStatus.newBuilder()
                            .setCode(ErrorStatus.Code.UNAVAILABLE)
                            .setMessage("Geofox API unavailable")
                            .setDetails(e.getMessage())
                            .build())
                    .build();

            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("[{}] Stations error: {}", correlationId, e.getMessage(), e);

            GetNearbyStationsResponse errorResponse = GetNearbyStationsResponse.newBuilder()
                    .setError(ErrorStatus.newBuilder()
                            .setCode(ErrorStatus.Code.INTERNAL)
                            .setMessage("Internal stations error")
                            .setDetails(e.getMessage())
                            .build())
                    .build();

            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getDeparturesForStations(GetDeparturesForStationsRequest request, StreamObserver<GetDeparturesForStationsResponse> responseObserver) {
        String correlationId = request.hasMeta() ? request.getMeta().getCorrelationId() : "unknown";


        log.info("[{}] GetDeparturesForStations request",
                correlationId);

        try {
            List<DepartureStation> stations = geofoxClient.getDeparturesForStations(
                    request.getStationsList());

            GetDeparturesForStationsResponse response = GetDeparturesForStationsResponse.newBuilder()
                    .setStations(DepartureStationList.newBuilder()
                            .addAllStations(stations)
                            .build())
                    .setResultMeta(ResultMeta.newBuilder()
                            .setGeneratedAt(Timestamp.newBuilder()
                                    .setSeconds(Instant.now().getEpochSecond())
                                    .build())
                            .setSource("Geofox/HVV")
                            .build())
                    .build();

            log.info("[{}] GetDeparturesForStations success: {} stations found",
                    correlationId, stations.size());

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (GeofoxClient.GeofoxApiException e) {
            log.warn("[{}] Geofox API error: {}", correlationId, e.getMessage());

            GetDeparturesForStationsResponse errorResponse = GetDeparturesForStationsResponse.newBuilder()
                    .setError(ErrorStatus.newBuilder()
                            .setCode(ErrorStatus.Code.UNAVAILABLE)
                            .setMessage("Geofox API unavailable")
                            .setDetails(e.getMessage())
                            .build())
                    .build();

            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("[{}] Departures error: {}", correlationId, e.getMessage(), e);

            GetDeparturesForStationsResponse errorResponse = GetDeparturesForStationsResponse.newBuilder()
                    .setError(ErrorStatus.newBuilder()
                            .setCode(ErrorStatus.Code.INTERNAL)
                            .setMessage("Internal departures error")
                            .setDetails(e.getMessage())
                            .build())
                    .build();

            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }
}