package vsp;

import com.google.protobuf.Timestamp;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DisplayModel {

    private final DisplayView view;
    private Address address;
    private List<DepartureStation> departures;

    public DisplayModel(DisplayView view) {
        this.view = view;
    }

    public void saveAddress(Address address) {
        this.address = address;
        view.notifyAddressSaved();
    }

    public void saveDepartures(List<DepartureStation> departures) {
        this.departures = departures;
        view.notifyDeparturesSaved();
    }

    public void displayDepartures(List<DepartureStation> departures) {
        displayDepartures(departures, null);
    }

    public void displayDepartures(List<DepartureStation> departures, Timestamp dataFetchedAt) {
        view.showDeparturesToMonitor(departures, dataFetchedAt);
    }

    public Address getAddress() {
        return address;
    }

    public List<DepartureStation> getDepartures() {
        return departures;
    }
}
