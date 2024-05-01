package com.okanatas.nfccardemulator;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

/**
 * This class was created for the Host Based Card Emulator Service.
 *
 * @author Okan Atas,
 * @version 1.0,
 * created on June 30, 2021
 */
public class HostCardEmulatorService extends HostApduService {

    private static final String C_TAG = InformationTransferManager.getStringResource(R.string.command_tag);
    private static final String R_TAG = InformationTransferManager.getStringResource(R.string.response_tag);
    private static final String COM_TAG = "\n" + InformationTransferManager.getStringResource(R.string.communication_tag);
    private static boolean isNewCommunication = true;

    /**
     * This method was created to take that command the terminal sends,
     * and then send back a response in the format that the terminal requires.
     *
     * @param commandApdu The APDU that was received from the remote device.
     * @param extras      a bundle containing extra data (may be null).
     * @return a byte-array containing the response APDU, or null if no response APDU can be sent at this point.
     */
    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        byte[] responseApdu;
        if (commandApdu != null) {
            Log.d("HostCardEmulatorService", "Got APDU: " + Utils.toHexString(commandApdu));
            String hexCommandApdu = Utils.toHexString(commandApdu);

            if ((hexCommandApdu.length() < ISOProtocol.MIN_APDU_LENGTH) || (hexCommandApdu.length() % 2 != 0)) {
                responseApdu = Utils.hexStringToByteArray(ISOProtocol.SW_COMMAND_ABORTED);
                ResponseHandler.setSelectedInsDescription(InformationTransferManager.getStringResource(R.string.command_aborted_with_reason));
            } else {
                Runnable response = ResponseHandler.getResponse(commandApdu, (apdu)->{
                    this.sendResponseApdu(apdu);
                    String commandApduIndex = (commandApdu != null) ? (Utils.toHexString(commandApdu)) : (InformationTransferManager.getStringResource(R.string.null_command));
                    handleCommunicationMessage(commandApduIndex, Utils.toHexString(apdu));
                });
                Thread sendResponse = new Thread(response);
                sendResponse.start();
                return null;
            }

        } else {
            responseApdu = Utils.hexStringToByteArray(ISOProtocol.SW_COMMAND_ABORTED);
        }

        String commandApduInHex = (commandApdu != null) ? (Utils.toHexString(commandApdu)) : (InformationTransferManager.getStringResource(R.string.null_command));
        handleCommunicationMessage(commandApduInHex, Utils.toHexString(responseApdu));
        return responseApdu;
    }

    /**
     * This method will be called in two possible scenarios:
     * 1) The NFC link has been deactivated or lost
     * 2) A different AID has been selected and was resolved to a different service component
     *
     * @param reason Either DEACTIVATION_LINK_LOSS or DEACTIVATION_DESELECTED
     */
    @Override
    public void onDeactivated(int reason) {
        Utils.showLogDMessage(COM_TAG, "\n" + InformationTransferManager.getStringResource(R.string.communication_ended) + "\n", true);
        isNewCommunication = true;
        MainScreenFragment.getInstance().showCommunicationMessages();
    }

    /**
     * This method handles the communication between the terminal and the emulated card.
     *
     * @param commandApduInHex  command APDU in hexadecimal format.
     * @param responseApduInHex response APDU in hexadecimal format.
     */
    private void handleCommunicationMessage(String commandApduInHex, String responseApduInHex) {

        // check if it is a new communication
        if (isNewCommunication) {
            Utils.showLogDMessage(COM_TAG, InformationTransferManager.getStringResource(R.string.communication_started) + "\n", true);
            isNewCommunication = false;
        }

        Utils.showLogDMessage(C_TAG, commandApduInHex, true);
//        Utils.showLogDMessage(C_TAG, ResponseHandler.getSelectedInsDescription(), false);
        Utils.showLogDMessage(R_TAG, responseApduInHex, true);

        InformationTransferManager.appendCardCommunicationMessage(InformationTransferManager.getStringResource(R.string.apdu_c) + commandApduInHex);
        InformationTransferManager.appendCardCommunicationMessage(InformationTransferManager.getStringResource(R.string.apdu_r) + responseApduInHex + "\n");

    }

}
