package org.cna.keyple.famoco.validator.viewModels;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import org.cna.keyple.famoco.validator.data.CardReaderApi;
import org.cna.keyple.famoco.validator.data.model.CardReaderResponse;
import org.cna.keyple.famoco.validator.data.model.Status;
import org.cna.keyple.famoco.validator.di.scopes.AppScoped;
import org.cna.keyple.famoco.validator.rx.SchedulerProvider;
import org.cna.keyple.famoco.validator.util.LiveEvent;
import org.eclipse.keyple.core.seproxy.event.ObservableReader;
import org.eclipse.keyple.core.seproxy.event.ReaderEvent;
import org.eclipse.keyple.core.seproxy.exception.KeypleBaseException;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;

@AppScoped
public class CardReaderViewModel extends ViewModel {
    private static final String TAG = CardReaderViewModel.class.getSimpleName();

    private final static String CALYPSO_PO_TYPE = "CALYPSO";

    /* application states */
    private enum AppState {
        UNSPECIFIED, WAIT_SYSTEM_READY, WAIT_CARD, CARD_STATUS
    }
    AppState currentAppState = AppState.WAIT_SYSTEM_READY;

    @NonNull
    private final CardReaderApi cardReaderApi;

    private final SchedulerProvider schedulerProvider;

    private final CompositeDisposable disposables = new CompositeDisposable();

    private final LiveEvent<CardReaderResponse> response = new LiveEvent<>();


    private boolean readersInitialized = false;

    @Inject
    public CardReaderViewModel(@NonNull CardReaderApi cardReaderApi, SchedulerProvider provider) {
        this.schedulerProvider = provider;
        this.cardReaderApi = cardReaderApi;
    }

    @Override
    protected void onCleared() {
        disposables.clear();
    }

    public LiveEvent<CardReaderResponse> getResponse() {
        return response;
    }

    /**
     * Generate observable to communicate with UI thread
     *
     * @param status
     * @param tickets
     */
    private void setUiResponse(Status status, Integer tickets, String contract, String cardType) {
        disposables.add(Observable.just(new CardReaderResponse(status, tickets, contract, cardType))
                .observeOn(schedulerProvider.ui())
                .subscribeOn(schedulerProvider.ui())
                .subscribe(response::setValue));
    }

    public void initCardReader() throws KeypleBaseException {
        if(!readersInitialized) {
            ObservableReader.ReaderObserver poReaderObserver = new PoObserver();
            cardReaderApi.init(poReaderObserver);
            handleAppEvents(AppState.WAIT_CARD, null);
            readersInitialized = true;
        }
    }

    /**
     * main app state machine handle
     *
     * @param appState
     * @param readerEvent
     */
    private void handleAppEvents(AppState appState, ReaderEvent readerEvent) {
//        Log.i(TAG, "Current state = " + currentAppState+ ", wanted new state = " + appState + ", event = " +
//                ((readerEvent == null) ? "null" : String.valueOf(readerEvent.getEventType())));
//        /* clear message */
//        //displayMessage("", 0);
//        if (readerEvent != null) {
//            if (readerEvent.getEventType().equals(ReaderEvent.EventType.SE_INSERTED) || readerEvent.getEventType().equals(ReaderEvent.EventType.SE_MATCHED)) {
//                if (appState == AppState.WAIT_SYSTEM_READY) {
//                    return;
//                }
//                Log.i(TAG, "Process default selection...");
////                imgAuthorized.setVisible(false);
////                imgUnauthorized.setVisible(false);
//                if (!ticketingSession.processDefaultSelection(readerEvent.getDefaultSelectionResponse())) {
//                    Log.e(TAG,"PO Not selected");
//                    setUiResponse(Status.INVALID_CARD, 0, "", ticketingSession.getPoTypeName());
////                    imgUnauthorized.setVisible(true);
////                    displayStatus("Card not supported in this demo");
////                    activatePane(PANE_CARD_STATUS);
//                    return;
//                }
//                Log.i(TAG, "PO Type = " + ticketingSession.getPoTypeName());
//                if (!ticketingSession.getPoTypeName().equals("CALYPSO")) {
//                      setUiResponse(Status.INVALID_CARD, 0, "", ticketingSession.getPoTypeName());
////                    imgUnauthorized.setVisible(true);
////                    displayStatus(ticketingSession.getPoTypeName() + " card" +
////                            " not supported in this demo");
////                    activatePane(PANE_CARD_STATUS);
//                    return;
//                } else {
////                    setUiResponse(Status.SUCCESS, 0, ticketingSession.getPoTypeName());
//                    Log.i(TAG,"A Calypso PO selection succeeded.");
//                    appState = AppState.CARD_STATUS;
//                }
//            } else if (readerEvent.getEventType().equals(ReaderEvent.EventType.SE_REMOVAL)) {
//                currentAppState = AppState.WAIT_SYSTEM_READY;
//            }
//        }
//        switch (appState) {
//            case WAIT_SYSTEM_READY:
////                activatePane(PANE_WAIT_SYSTEM_READY);
//                currentAppState = appState;
//                break;
//            case WAIT_CARD:
//                if (currentAppState != appState) {
////                    activatePane(PANE_WAIT_CARD);
//                    currentAppState = appState;
//                }
//                break;
//            case CARD_STATUS:
//                currentAppState = AppState.CARD_STATUS;
//                if (readerEvent != null && (readerEvent.getEventType().equals(ReaderEvent.EventType.SE_INSERTED) || readerEvent.getEventType().equals(ReaderEvent.EventType.SE_MATCHED))) {
//                    try {
//                        if (ticketingSession.analyzePoProfile()) {
//                            String contract = "";
//                            CardContent cardContent = ticketingSession.getCardContent();
//                            if(cardContent.getContracts().get(1) != null) {
//                                contract = new String(cardContent.getContracts().get(1));
//                            }
//                            Log.i(TAG,"Contract = " + contract);
//                            if(contract.isEmpty() || contract.contains("NO CONTRACT") || !contract.contains(
//                                    "SEASON")) {
//                                if(cardContent.getCounters().get(0) > 0) {
//                                    Log.i(TAG, "Load TICKETS_FOUND page.");
//                                    setUiResponse(Status.TICKETS_FOUND, ticketingSession.getCardContent().getCounters().get(0), "", ticketingSession.getPoTypeName());
//                                } else {
//                                    Log.i(TAG, "Load EMPTY_CARD page.");
//                                    setUiResponse(Status.EMPTY_CARD, 0, "", ticketingSession.getPoTypeName());
//                                }
//                            } else {
//                                Log.i(TAG,"Load TICKETS_FOUND page.");
//                                setUiResponse(Status.TICKETS_FOUND, 0, contract, ticketingSession.getPoTypeName());
//                            }
////                            switch (ticketingSession.loadTickets(0)) {
////                                case TicketingSession.STATUS_OK:
////                                    break;
////                                case TicketingSession.STATUS_UNKNOWN_ERROR:
////                                case TicketingSession.STATUS_CARD_SWITCHED:
////                                    setUiResponse(Status.ERROR, 0, ticketingSession.getPoTypeName());
//////                                    imgUnauthorized.setVisible(true);
//////                                    displayStatus("Unknown error");
////                                    break;
////                                case TicketingSession.STATUS_SESSION_ERROR:
////                                    setUiResponse(Status.ERROR, 0, ticketingSession.getPoTypeName());
//////                                    imgUnauthorized.setVisible(true);
//////                                    displayStatus("Secure Session error");
////                                    break;
////                            }
//                        }
////                    } catch (KeypleReaderException e) {
////                        setUiResponse(Status.ERROR, 0, ticketingSession.getPoTypeName());
////                        e.printStackTrace();
//                    } catch (IllegalStateException e) {
//                        Log.i(TAG,"Load ERROR page after exception = " + e.getMessage());
//                        setUiResponse(Status.ERROR, 0, "", ticketingSession.getPoTypeName());
//                        e.printStackTrace();
//                    }
//                }
//                break;
//        }
//        Log.i(TAG,"New state = " + currentAppState);
    }

    private class PoObserver implements ObservableReader.ReaderObserver {
        @Override
        public void update(ReaderEvent event) {
            Log.i(TAG, "New ReaderEvent received : " + event.getEventType());
            handleAppEvents(currentAppState, event);
        }
    }
}

