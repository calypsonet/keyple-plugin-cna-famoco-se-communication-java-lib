package org.cna.keyple.famoco.validator.util;

public class CalypsoClassicInfo {
    /**
     * Calypso default AID
     */
    public final static String AID = "A0000004040125090101";
    /// ** 1TIC.ICA AID */
    // public final static String AID = "315449432E494341";
    /**
     * SAM C1 regular expression: platform, version and serial number values are ignored
     */
    public final static String SAM_C1_ATR_REGEX =
            "3B3F9600805A[0-9a-fA-F]{2}80C1[0-9a-fA-F]{14}829000";

    public final static String ATR_REV1_REGEX = "3B8F8001805A0A0103200311........829000..";

    public final static byte RECORD_NUMBER_1 = 1;
    public final static byte RECORD_NUMBER_2 = 2;
    public final static byte RECORD_NUMBER_3 = 3;
    public final static byte RECORD_NUMBER_4 = 4;

    public final static byte SFI_EnvironmentAndHolder = (byte) 0x07;
    public final static byte SFI_EventLog = (byte) 0x08;
    public final static byte SFI_ContractList = (byte) 0x1E;
    public final static byte SFI_Contracts = (byte) 0x09;
    public final static byte SFI_Counter1 = (byte) 0x19;


    public final static String eventLog_dataFill =
            "00112233445566778899AABBCCDDEEFF00112233445566778899AABBCC";

    public CalypsoClassicInfo() {
    }
}
