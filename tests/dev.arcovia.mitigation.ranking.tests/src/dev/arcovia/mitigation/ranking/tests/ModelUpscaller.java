package dev.arcovia.mitigation.ranking.tests;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModelUpscaller {

	public static void main(String[] args) {
        String input = "<sources xsi:type=\"dfd:DFDComponentUncertaintySource\" id=\"_haiktnQSEe-hMp-vXVNX6A\" entityName=\"Bank_Clerk_View_uncertain\">\r\n"
        		+ "    <target xsi:type=\"dataflowdiagram:External\" href=\"OBM.dataflowdiagram#_CqeuEVsqEe-imMDNUFdrZA\"/>\r\n"
        		+ "    <scenarios id=\"_mBN04HQSEe-hMp-vXVNX6A\">\r\n"
        		+ "      <target xsi:type=\"dataflowdiagram:Store\" href=\"OBM.dataflowdiagram#_W3GbInQSEe-hMp-vXVNX6A\"/>\r\n"
        		+ "    </scenarios>\r\n"
        		+ "  </sources>\r\n"
        		+ "  <sources xsi:type=\"dfd:DFDExternalUncertaintySource\" id=\"_RWfdZ1c2Ee-m2-jCTo323w\" entityName=\"CBD_Uncertain\">\r\n"
        		+ "    <target xsi:type=\"dataflowdiagram:Store\" href=\"OBM.dataflowdiagram#9zxnz\"/>\r\n"
        		+ "    <targetProperties href=\"OBM.datadictionary#5hnugm\"/>\r\n"
        		+ "    <scenarios id=\"_WuPIsFc3Ee-ljYOj8Tf2YQ\">\r\n"
        		+ "      <targetProperties href=\"OBM.datadictionary#g10hr\"/>\r\n"
        		+ "    </scenarios>\r\n"
        		+ "  </sources>\r\n"
        		+ "  <sources xsi:type=\"dfd:DFDInterfaceUncertaintySource\" id=\"_WFOdaVc2Ee-m2-jCTo323w\" entityName=\"plo_Uncertain\">\r\n"
        		+ "    <targetFlow href=\"OBM.dataflowdiagram#fmfdo\"/>\r\n"
        		+ "    <scenarios id=\"_aHx3YFc3Ee-ljYOj8Tf2YQ\">\r\n"
        		+ "      <targetInPin href=\"OBM.datadictionary#42dbbn\"/>\r\n"
        		+ "      <targetNode xsi:type=\"dataflowdiagram:External\" href=\"OBM.dataflowdiagram#i0jwe\"/>\r\n"
        		+ "    </scenarios>\r\n"
        		+ "  </sources>\r\n"
        		+ "  <sources xsi:type=\"dfd:DFDBehaviorUncertaintySource\" id=\"_yKdCuFc2Ee-m2-jCTo323w\" entityName=\"show_account_state_uncertain\">\r\n"
        		+ "    <target href=\"OBM.datadictionary#__SzOeFc1Ee-aJpuBt44G8A\"/>\r\n"
        		+ "    <targetAssignments xsi:type=\"datadictionary:ForwardingAssignment\" href=\"OBM.datadictionary#__S6jMVc1Ee-aJpuBt44G8A\"/>\r\n"
        		+ "    <scenarios id=\"_DDuF4Fc3Ee-m2-jCTo323w\">\r\n"
        		+ "      <targetAssignments xsi:type=\"datadictionary:Assignment\" href=\"OBM.datadictionary#_k6awAlc2Ee-m2-jCTo323w\"/>\r\n"
        		+ "    </scenarios>\r\n"
        		+ "  </sources>\r\n"
        		+ "  <sources xsi:type=\"dfd:DFDExternalUncertaintySource\" id=\"_i_u88VifEe-01fJ0Yo6HNg\" entityName=\"Customer_Location_Uncertain\">\r\n"
        		+ "    <target xsi:type=\"dataflowdiagram:External\" href=\"OBM.dataflowdiagram#dxnng9\"/>\r\n"
        		+ "    <targetProperties href=\"OBM.datadictionary#g10hr\"/>\r\n"
        		+ "    <scenarios id=\"_mCfjUFifEe-01fJ0Yo6HNg\">\r\n"
        		+ "      <targetProperties href=\"OBM.datadictionary#5hnugm\"/>\r\n"
        		+ "    </scenarios>\r\n"
        		+ "  </sources>\r\n"
        		+ "  <sources xsi:type=\"dfd:DFDExternalUncertaintySource\" id=\"_p2OTqFifEe-01fJ0Yo6HNg\" entityName=\"Customer_Settings_Data_Location\">\r\n"
        		+ "    <target xsi:type=\"dataflowdiagram:Store\" href=\"OBM.dataflowdiagram#_e1yhglieEe-01fJ0Yo6HNg\"/>\r\n"
        		+ "    <targetProperties href=\"OBM.datadictionary#g10hr\"/>\r\n"
        		+ "    <scenarios id=\"_v6mQ4FifEe-01fJ0Yo6HNg\">\r\n"
        		+ "      <targetProperties href=\"OBM.datadictionary#5hnugm\"/>\r\n"
        		+ "    </scenarios>\r\n"
        		+ "  </sources>\r\n"
        		+ "  <sources xsi:type=\"dfd:DFDComponentUncertaintySource\" id=\"_K_npgVi4Ee-DsdRrhZRfUQ\" entityName=\"Developer_View_Uncertain\">\r\n"
        		+ "    <target xsi:type=\"dataflowdiagram:Store\" href=\"OBM.dataflowdiagram#b9ernl\"/>\r\n"
        		+ "    <scenarios id=\"_O5dB0Fi4Ee-DsdRrhZRfUQ\">\r\n"
        		+ "      <target xsi:type=\"dataflowdiagram:Store\" href=\"OBM.dataflowdiagram#_MvXlUli3Ee-DsdRrhZRfUQ\"/>\r\n"
        		+ "    </scenarios>\r\n"
        		+ "  </sources>\r\n"
        		+ "  <sources xsi:type=\"dfd:DFDComponentUncertaintySource\" id=\"_x-556FsvEe-imMDNUFdrZA\" entityName=\"Fond_Data_Uncertain\">\r\n"
        		+ "    <target xsi:type=\"dataflowdiagram:Store\" href=\"OBM.dataflowdiagram#_OCehElsqEe-imMDNUFdrZA\"/>\r\n"
        		+ "    <scenarios id=\"_5-x6gFsvEe-imMDNUFdrZA\">\r\n"
        		+ "      <target xsi:type=\"dataflowdiagram:Store\" href=\"OBM.dataflowdiagram#_SYeQ8lsqEe-imMDNUFdrZA\"/>\r\n"
        		+ "    </scenarios>\r\n"
        		+ "  </sources>\r\n"
        		+ "  <sources xsi:type=\"dfd:DFDConnectorUncertaintySource\" id=\"_7VOwG1svEe-imMDNUFdrZA\" entityName=\"get_fond_data_uncertain\">\r\n"
        		+ "    <targetAssignments xsi:type=\"datadictionary:Assignment\" href=\"OBM.datadictionary#_n2ZdAlsoEe-imMDNUFdrZA\"/>\r\n"
        		+ "    <targetFlow href=\"OBM.dataflowdiagram#_glyEMFsqEe-imMDNUFdrZA\"/>\r\n"
        		+ "    <scenarios id=\"_D2CTgFswEe-imMDNUFdrZA\">\r\n"
        		+ "      <targetAssignments xsi:type=\"datadictionary:Assignment\" href=\"OBM.datadictionary#_yWbtQlsoEe-imMDNUFdrZA\"/>\r\n"
        		+ "      <targetPin href=\"OBM.datadictionary#_Ks1QYFspEe-imMDNUFdrZA\"/>\r\n"
        		+ "      <targetNode xsi:type=\"dataflowdiagram:External\" href=\"OBM.dataflowdiagram#_JndHQVsqEe-imMDNUFdrZA\"/>\r\n"
        		+ "    </scenarios>\r\n"
        		+ "  </sources>";
        String regex = "("
        		+ "Store\" href|External\" href|Process\" href|target href|Assignment\" href|targetInPin href|targetFlow href|targetNode href|"
        		+ "behaviour href|destinationPin href|sourcePin href|sourceNode|destinationNode"
        		+ "|id|inputPins|outputPin|entityName)"
        		+ "=\"([^\"]*)\""; // Example regex to match all sequences of digits

        // Compile the regex
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        // Create a StringBuffer to hold the result
        StringBuffer result = new StringBuffer();

        // Replace all matches
        while (matcher.find()) {
            String match = matcher.group(); // Get the matched value
            matcher.appendReplacement(result,  match.substring(0, match.length() - 1) + "_2\""); // Append original value with "_1"
        }
        matcher.appendTail(result); // Append the remaining part of the input string

        // Output the result
        System.out.println(result.toString());
    }
	
	
}
