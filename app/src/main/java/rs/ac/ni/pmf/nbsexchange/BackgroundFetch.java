package rs.ac.ni.pmf.nbsexchange;

import android.os.AsyncTask;
import android.util.Log;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;

import java.util.ArrayList;
import java.util.Arrays;

class BackgroundFetch extends AsyncTask<String, Integer, String> {

    public static Element[] buildAuthHeader(String NAMESPACE, String soap_username, String soap_password, String soap_licenceID) {
        Element headers[] = new Element[1];
        headers[0] = new Element().createElement(NAMESPACE, "AuthenticationHeader");
        headers[0].setAttribute(NAMESPACE, "xmlns", NAMESPACE);
        Element security = headers[0];


        //username
        Element username = new Element().createElement(security.getNamespace(), "UserName");
        username.addChild(Node.TEXT, soap_username);
        security.addChild(Node.ELEMENT, username);

        // password
        Element password = new Element().createElement(security.getNamespace(), "Password");
        password.addChild(Node.TEXT, soap_password);
        security.addChild(Node.ELEMENT, password);

        // licenceID
        Element licence = new Element().createElement(security.getNamespace(), "LicenceID");
        licence.addChild(Node.TEXT, soap_licenceID);
        security.addChild(Node.ELEMENT, licence);
        return headers;
    }

    private static ArrayList<SoapSerializationEnvelope> createEnvelopesForDates(String NAMESPACE, String methodName, String soap_username, String soap_password, String soap_licenceID) {

        ArrayList<SoapSerializationEnvelope> envelopes = new ArrayList<>();
        String[] dates = MainActivity.getPreviousDates(MainActivity.nDates);

        for (String date : dates) {

            SoapObject request = new SoapObject(NAMESPACE, methodName);
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.enc = "utf-8";
            envelope.version = 1;
            envelope.xsi = "http://www.w3.org/2001/XMLSchema-instance";
            envelope.xsd = "http://www.w3.org/2001/XMLSchema";
            envelope.dotNet = true;

            envelope.setOutputSoapObject(request);
            envelope.headerOut = buildAuthHeader(NAMESPACE, soap_username, soap_password, soap_licenceID);


            PropertyInfo propD = new PropertyInfo(), propT = new PropertyInfo();

            propD.setName("date");
            propD.setValue(date);
            propD.setType(String.class);
            request.addProperty(propD);


            propT.setName("exchangeRateListTypeID");
            propT.setValue(2);
            propT.setType(Integer.class);
            request.addProperty(propT);

            envelopes.add(envelope);
        }

        return envelopes;
    }

    private static ArrayList<String> fetchData(String URL, String NAMESPACE, String methodName, String soap_action, String soap_username, String soap_password, String soap_licenceID) {

        //Initialize soap request + add parameters

        HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);

        ArrayList<SoapSerializationEnvelope> envelopes = createEnvelopesForDates(NAMESPACE, methodName, soap_username, soap_password, soap_licenceID);

        ArrayList<String> xmlResponses = new ArrayList<String>();


        androidHttpTransport.debug = true;

        try {
            for (SoapSerializationEnvelope envelope : envelopes) {

                androidHttpTransport.call(soap_action, envelope);

                SoapPrimitive response = (SoapPrimitive) envelope.getResponse();
                xmlResponses.add(response.toString());

            }
        } catch (SoapFault e) {
            e.printStackTrace();

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return xmlResponses;

    }

    @Override
    protected String doInBackground(String... params) {

        ArrayList<String> response = BackgroundFetch.fetchData(params[0], params[1], params[2], params[3], params[4], params[5], params[6]);

        return (response != null) ? response.toString() : null;
    }
};
