package com.okanatas.nfccardemulator;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;

/**
 * This class was created to select responses for command APDU.
 *
 * @author Okan Atas,
 * @version 1.0,
 * created on June 30, 2021
 */
public class ResponseHandler {

    private static String selectedInsDescription;

    private static final NetworkService networkService = new NetworkService();
    private static boolean isUsingNetwork = false;

    public static void setUsingNetwork(boolean isUsingNetwork) {
        ResponseHandler.isUsingNetwork = isUsingNetwork;
        if (isUsingNetwork) {
            networkService.connect();
        } else {
            networkService.disconnect();
        }
    }

    public static boolean isUsingNetwork() {
        return isUsingNetwork;
    }


    public interface ResponseHandlerInterface {
        void onResponseReceived(byte[] responseApdu);
    }

    /**
     * This method was created to get the response of the command APDU in asynchronous way in case
     * the file handler way is used
     *
     * @param commandApdu     command APDU in byte array format.
     * @param responseHandler response handler interface.
     * @return a runnable object to handle the response asynchronously.
     */
    @NonNull
    @Contract(pure = true)
    private static Runnable getFileHandlerResponse(byte[] commandApdu, ResponseHandlerInterface responseHandler) {
        return () -> {
            byte[] responseApdu;

            if (commandApdu != null) {

                String hexCommandApdu = Utils.toHexString(commandApdu);

                if ((hexCommandApdu.length() < ISOProtocol.MIN_APDU_LENGTH) || (hexCommandApdu.length() % 2 != 0)) {
                    responseApdu = Utils.hexStringToByteArray(ISOProtocol.SW_COMMAND_ABORTED);
                    ResponseHandler.setSelectedInsDescription(InformationTransferManager.getStringResource(R.string.command_aborted_with_reason));
                } else {
                    /* Switch for INS : this is the index 1 for commandApdu byte array */
                    switch (hexCommandApdu.substring(2, 4)) {
                        case ISOProtocol.INS_SELECT:
                            responseApdu = ResponseHandler.selectCase(hexCommandApdu);
                            break;
                        case ISOProtocol.INS_READ_BINARY:
                            responseApdu = ResponseHandler.readBinaryCase();
                            break;
                        case ISOProtocol.INS_WRITE_BINARY:
                            responseApdu = ResponseHandler.writeBinaryCase();
                            break;
                        case ISOProtocol.INS_UPDATE_BINARY:
                            responseApdu = ResponseHandler.updateBinaryCase();
                            break;
                        case ISOProtocol.INS_READ_RECORD:
                            responseApdu = ResponseHandler.readRecordCase(hexCommandApdu);
                            break;
                        case ISOProtocol.INS_READ_NDEF:
                            responseApdu = ResponseHandler.readNdefCase();
                            break;
                        case ISOProtocol.INS_PERFORM_SECURITY_OPERATION:
                            responseApdu = ResponseHandler.performSecurityOperationCase();
                            break;
                        case ISOProtocol.INS_GET_PROCESSING_OPTIONS:
                            responseApdu = ResponseHandler.getProcessingOptionCase(hexCommandApdu);
                            break;
                        case ISOProtocol.INS_GENERATE_APPLICATION_CRYPTOGRAM:
                            responseApdu = ResponseHandler.generateApplicationCryptogramCase();
                            break;
                        case ISOProtocol.INS_GET_DATA:
                            responseApdu = ResponseHandler.getDataCase();
                            break;
                        default:
                            responseApdu = Utils.hexStringToByteArray(ISOProtocol.SW_INS_NOT_SUPPORTED_OR_INVALID);
                    }
                }

            } else {
                responseApdu = Utils.hexStringToByteArray(ISOProtocol.SW_COMMAND_ABORTED);
            }
            responseHandler.onResponseReceived(responseApdu);
        };
    }

    /**
     * This method was created to get the response of the command APDU in asynchronous way in case
     * the network way is used
     *
     * @param commandApdu     command APDU in byte array format.
     * @param responseHandler response handler interface.
     * @return a runnable object to handle the response asynchronously.
     */
    private static Runnable getNetworkResponse(byte[] commandApdu, ResponseHandlerInterface responseHandler) {
        return () -> {
            networkService.sendCommand(commandApdu);
            byte[] responseApdu = networkService.waitForResponse();
            responseHandler.onResponseReceived(responseApdu);
        };
    }

    public static Runnable getResponse(byte[] commandApdu, ResponseHandlerInterface responseHandler) {
        if (isUsingNetwork) {
            return getNetworkResponse(commandApdu, responseHandler);
        } else {
            return getFileHandlerResponse(commandApdu, responseHandler);
        }
    }

    /**
     * Command APDU - Select Case.
     *
     * @param hexCommandApdu command APDU in hexadecimal format.
     * @return response APDU.
     */
    private static byte[] selectCase(String hexCommandApdu) {
        byte[] responseApdu = Utils.hexStringToByteArray(ISOProtocol.SW_FILE_NOT_FOUND);
        selectedInsDescription = InformationTransferManager.getStringResource(R.string.ins_select_case);

        for (int i = 0; i < FileHandler.commands.size(); i++) {
            if (hexCommandApdu.equals(FileHandler.commands.get(i))) {
                responseApdu = Utils.hexStringToByteArray(FileHandler.responses.get(i));
                break;
            }
        }
        return responseApdu;
    }

    /**
     * Command APDU - Read Binary Case.
     *
     * @return response APDU.
     */
    private static byte[] readBinaryCase() {
        byte[] responseApdu = Utils.hexStringToByteArray(ISOProtocol.SW_RECORD_NOT_FOUND);
        selectedInsDescription = InformationTransferManager.getStringResource(R.string.ins_read_binary);

        for (int i = 0; i < FileHandler.commands.size(); i++) {
            if (ISOProtocol.INS_READ_BINARY.equals(FileHandler.commands.get(i).substring(2, 4))) {
                responseApdu = Utils.hexStringToByteArray(FileHandler.responses.get(i));
                break;
            }
        }
        return responseApdu;
    }

    /**
     * Command APDU - Get Processing Option Case.
     *
     * @param hexCommandApdu command APDU in hexadecimal format.
     * @return response APDU.
     */
    private static byte[] getProcessingOptionCase(String hexCommandApdu) {
        byte[] responseApdu = Utils.hexStringToByteArray(ISOProtocol.SW_COMMAND_ABORTED);
        selectedInsDescription = InformationTransferManager.getStringResource(R.string.ins_get_processing_option);

        for (int i = 0; i < FileHandler.commands.size(); i++) {
            if (hexCommandApdu.equals(FileHandler.commands.get(i))) {
                responseApdu = Utils.hexStringToByteArray(FileHandler.responses.get(i));
                break;
            }
        }
        return responseApdu;
    }

    /**
     * Command APDU - Read Record Case.
     *
     * @param hexCommandApdu command APDU in hexadecimal format.
     * @return response APDU.
     */
    private static byte[] readRecordCase(String hexCommandApdu) {
        byte[] responseApdu = Utils.hexStringToByteArray(ISOProtocol.SW_RECORD_NOT_FOUND);
        selectedInsDescription = InformationTransferManager.getStringResource(R.string.ins_read_record);

        for (int i = 0; i < FileHandler.commands.size(); i++) {
            if (hexCommandApdu.equals(FileHandler.commands.get(i))) {
                responseApdu = Utils.hexStringToByteArray(FileHandler.responses.get(i));
                break;
            }
        }
        return responseApdu;
    }

    /**
     * Command APDU - Perform Security Operation Case.
     *
     * @return response APDU.
     */
    private static byte[] performSecurityOperationCase() {
        byte[] responseApdu = Utils.hexStringToByteArray(ISOProtocol.SW_COMMAND_ABORTED);
        selectedInsDescription = InformationTransferManager.getStringResource(R.string.ins_perform_security);

        for (int i = 0; i < FileHandler.commands.size(); i++) {
            if (ISOProtocol.INS_PERFORM_SECURITY_OPERATION.equals(FileHandler.commands.get(i).substring(2, 4))) {
                responseApdu = Utils.hexStringToByteArray(FileHandler.responses.get(i));
                break;
            }
        }
        return responseApdu;
    }

    /**
     * Command APDU - Read NDEF Case.
     *
     * @return response APDU.
     */
    private static byte[] readNdefCase() {
        byte[] responseApdu = Utils.hexStringToByteArray(ISOProtocol.SW_COMMAND_ABORTED);
        selectedInsDescription = InformationTransferManager.getStringResource(R.string.ins_read_ndef);

        for (int i = 0; i < FileHandler.commands.size(); i++) {
            if (ISOProtocol.INS_READ_NDEF.equals(FileHandler.commands.get(i).substring(2, 4))) {
                responseApdu = Utils.hexStringToByteArray(FileHandler.responses.get(i));
                break;
            }
        }
        return responseApdu;
    }

    /**
     * Command APDU - Write Binary Case.
     *
     * @return response APDU.
     */
    private static byte[] writeBinaryCase() {
        byte[] responseApdu = Utils.hexStringToByteArray(ISOProtocol.SW_COMMAND_ABORTED);
        selectedInsDescription = InformationTransferManager.getStringResource(R.string.ins_write_binary);

        for (int i = 0; i < FileHandler.commands.size(); i++) {
            if (ISOProtocol.INS_WRITE_BINARY.equals(FileHandler.commands.get(i).substring(2, 4))) {
                responseApdu = Utils.hexStringToByteArray(FileHandler.responses.get(i));
                break;
            }
        }
        return responseApdu;
    }

    /**
     * Command APDU - Update Binary Case.
     *
     * @return response APDU.
     */
    private static byte[] updateBinaryCase() {
        byte[] responseApdu = Utils.hexStringToByteArray(ISOProtocol.SW_COMMAND_ABORTED);
        selectedInsDescription = InformationTransferManager.getStringResource(R.string.ins_update_binary);

        for (int i = 0; i < FileHandler.commands.size(); i++) {
            if (ISOProtocol.INS_UPDATE_BINARY.equals(FileHandler.commands.get(i).substring(2, 4))) {
                responseApdu = Utils.hexStringToByteArray(FileHandler.responses.get(i));
                break;
            }
        }
        return responseApdu;
    }

    /**
     * Command APDU - Generate Application Cryptogram Case.
     *
     * @return response APDU.
     */
    private static byte[] generateApplicationCryptogramCase() {
        byte[] responseApdu = Utils.hexStringToByteArray(ISOProtocol.SW_COMMAND_ABORTED);
        selectedInsDescription = InformationTransferManager.getStringResource(R.string.ins_generate_app_cryptogram);

        for (int i = 0; i < FileHandler.commands.size(); i++) {
            if (ISOProtocol.INS_GENERATE_APPLICATION_CRYPTOGRAM.equals(FileHandler.commands.get(i).substring(2, 4))) {
                responseApdu = Utils.hexStringToByteArray(FileHandler.responses.get(i));
                break;
            }
        }
        return responseApdu;
    }

    /**
     * Command APDU - Get Data Case.
     *
     * @return response APDU.
     */
    private static byte[] getDataCase() {
        byte[] responseApdu = Utils.hexStringToByteArray(ISOProtocol.SW_COMMAND_ABORTED);
        selectedInsDescription = InformationTransferManager.getStringResource(R.string.ins_get_data);

        for (int i = 0; i < FileHandler.commands.size(); i++) {
            if (ISOProtocol.INS_GET_DATA.equals(FileHandler.commands.get(i).substring(2, 4))) {
                responseApdu = Utils.hexStringToByteArray(FileHandler.responses.get(i));
                break;
            }
        }
        return responseApdu;
    }

    /**
     * To get selected INS description.
     *
     * @return selected INS description.
     */
    static String getSelectedInsDescription() {
        return selectedInsDescription;
    }

    /**
     * To set selected INS description.
     *
     * @param description INS description.
     */
    static void setSelectedInsDescription(String description) {
        selectedInsDescription = description;
    }

}
