package rs.ac.ni.pmf.nbsexchange;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;

import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.achartengine.chart.BarChart;
import org.achartengine.model.RangeCategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.achartengine.ChartFactory;

import java.util.Calendar;
import java.util.TreeMap;

import android.icu.text.*;
import android.widget.Toast;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends AppCompatActivity {

    static final String URL = "https://webservices.nbs.rs/CommunicationOfficeService1_0/ExchangeRateXmlService.asmx?WSDL";
    static final String NAMESPACE = "http://communicationoffice.nbs.rs";
    static final String methodName = "GetExchangeRateByDate";
    static final String soap_action = "http://communicationoffice.nbs.rs/GetExchangeRateByDate";
    static final String soap_username = "*"; //INSERT YOUR CREDENTIALS HERE
    static final String soap_password = "*"; //INSERT YOUR CREDENTIALS HERE
    static final String soap_licenceID = "*"; //INSERT YOUR CREDENTIALS HERE
    static final byte nDates = 30;
    static BackgroundFetch myTask = null;
    final String[] currencies = {"CHF", "EUR", "USD"};
    final String[] samoRSD = {"", "RSD", ""};
    static TreeMap<Date, TreeMap<String, Double[]>> values = new TreeMap<>();
    static String response = null;
    final static SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
    final static Calendar cal = Calendar.getInstance();
    static Date last = new Date();
    static boolean dontChange = false;
    static Integer oldScrollValue = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final NumberPicker np = (NumberPicker) findViewById(R.id.np);
        final NumberPicker np2 = (NumberPicker) findViewById(R.id.np2);
        final TextView textProdajni = (TextView) findViewById(R.id.textP);
        final TextView textSrednji = (TextView) findViewById(R.id.textS);
        final TextView textKupovni = (TextView) findViewById(R.id.textK);
        final EditText editText = (EditText) findViewById(R.id.editText);
        final EditText editText2 = (EditText) findViewById(R.id.editText2);
        final RadioGroup rg = (RadioGroup) findViewById(R.id.radioGrouoPSK);
        final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        final TextView datumText = (TextView) findViewById(R.id.datumText);
        final RangeCategorySeries series = new RangeCategorySeries("Exchange rate in preceding days");
        final XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
        final LinearLayout swipe_layout = (LinearLayout) findViewById(R.id.linLayScorll);


        mRenderer.setBarSpacing(1);
        mRenderer.setBarWidth(50f);
        mRenderer.setPanEnabled(true, false);
        mRenderer.setZoomEnabled(true, false);
        mRenderer.setXLabels(0);
        mRenderer.setShowLegend(false);
        mRenderer.setLabelsTextSize(25);

        last.setHours(0);
        last.setMinutes(0);
        last.setSeconds(0);
        last.setTime(last.getTime() - last.getTime() % 1000); //Millisekunden auf 0

        try {
            if (values.keySet().size() == 0)
                fetchData();

            last = maxFetchedDate();
            datumText.setText(sdf.format(last) + ".");

            updateUI(swipe_layout, mRenderer, datumText, last, np, np2, textProdajni, textSrednji, textKupovni, editText, editText2, rg);
        } catch (Exception e) {

            editText.setEnabled(false);
            editText2.setEnabled(false);
            for (int i = 0; i < rg.getChildCount(); i++) {
                rg.getChildAt(i).setEnabled(false);
            }
        }


        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeLayout.setEnabled(false);

                try {
                    fetchData();
                    last = maxFetchedDate();
                    updateUI(swipe_layout, mRenderer, datumText, last, np, np2, textProdajni, textSrednji, textKupovni, editText, editText2, rg);
                    editText.setEnabled(true);
                    editText2.setEnabled(true);
                    for (int i = 0; i < rg.getChildCount(); i++) {
                        rg.getChildAt(i).setEnabled(true);
                    }

                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "No updates made", Toast.LENGTH_SHORT).show();
                    editText.setEnabled(false);
                    editText2.setEnabled(false);
                    for (int i = 0; i < rg.getChildCount(); i++) {
                        rg.getChildAt(i).setEnabled(false);
                    }
                }
                swipeLayout.setRefreshing(false);
                swipeLayout.setEnabled(true);

            }

        });


        np2.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {

                textKupovni.setText(Double.toString(values.get(last).get(currencies[newVal])[0]));
                textSrednji.setText(Double.toString(values.get(last).get(currencies[newVal])[1]));
                textProdajni.setText(Double.toString(values.get(last).get(currencies[newVal])[2]));

                boolean leviOn = editText.isFocused();

                editText2.clearFocus();
                editText.clearFocus();
                dontChange = true;
                //double rightValue = Double.valueOf(editText2.getText().toString());
                double course = values.get(last).get(currencies[newVal])[rg.indexOfChild((RadioButton) findViewById(rg.getCheckedRadioButtonId()))];

                if (leviOn) {
                    if (editText.getText().toString().length() != 0)
                        editText2.setText(Double.toString(Double.valueOf(editText.getText().toString()) / course));
                    else
                        editText2.setText("0.0");

                    editText.requestFocus();
                } else {
                    if (editText2.getText().toString().length() != 0)
                        editText.setText(Double.toString(Double.valueOf(editText2.getText().toString()) * course));
                    else
                        editText.setText("0.0");
                    editText2.requestFocus();
                }

                updateChart(swipe_layout, mRenderer, np2.getDisplayedValues()[np2.getValue()]);
                dontChange = false;
                oldScrollValue = newVal;
            }
        });


        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {


                if (getCurrentFocus() == editText)
                    if (editText.getText().toString().length() != 0)
                        editText2.setText(Double.toString(Double.valueOf(editText.getText().toString()) /
                                values.get(last).get(currencies[np2.getValue()])[rg.indexOfChild((RadioButton) findViewById(rg.getCheckedRadioButtonId()))]));
                    else
                        editText2.setText("0.0");
                else if (getCurrentFocus() == editText2)
                    if (editText2.getText().toString() != "")
                        editText.setText(Double.toString(Double.valueOf(editText2.getText().toString()) * values.get(last).get(currencies[np2.getValue()])[rg.indexOfChild((RadioButton) findViewById(rg.getCheckedRadioButtonId()))]));
                    else
                        editText.setText("0.0");
            }

        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (dontChange)
                    return;
                if (getCurrentFocus() != editText)
                    return;
                if (charSequence.length() == 0)
                    editText2.setText("0.0");
                else {
                    final StringBuilder sb = new StringBuilder(charSequence.length());
                    sb.append(charSequence);

                    editText2.setText(Double.toString(Double.valueOf(editText.getText().toString()) /
                            values.get(last).get(currencies[np2.getValue()])[rg.indexOfChild((RadioButton) findViewById(rg.getCheckedRadioButtonId()))]));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        editText2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (dontChange)
                    return;
                if (getCurrentFocus() != editText2)
                    return;
                if (charSequence.length() == 0)
                    editText.setText("0.0");
                else {
                    final StringBuilder sb = new StringBuilder(charSequence.length());
                    sb.append(charSequence);
                    editText.setText(Double.toString(Double.valueOf(editText2.getText().toString()) * values.get(last).get(currencies[np2.getValue()])[rg.indexOfChild((RadioButton) findViewById(rg.getCheckedRadioButtonId()))]));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    private Date maxFetchedDate() {

        Date ret = null;

        for (Date d : values.keySet())
            if (ret == null || d.after(ret))
                ret = d;
        return ret;
    }

    private static boolean setNumberPickerTextColor(NumberPicker numberPicker, int color) {
        final int count = numberPicker.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = numberPicker.getChildAt(i);
            if (child instanceof EditText) {
                try {
                    Field selectorWheelPaintField = numberPicker.getClass()
                            .getDeclaredField("mSelectorWheelPaint");
                    selectorWheelPaintField.setAccessible(true);
                    ((Paint) selectorWheelPaintField.get(numberPicker)).setColor(color);
                    ((EditText) child).setTextColor(color);
                    numberPicker.invalidate();
                    return true;
                } catch (NoSuchFieldException e) {
                    Log.w("NumberPickerTextColor", e);
                } catch (IllegalAccessException e) {
                    Log.w("NumberPickerTextColor", e);
                } catch (IllegalArgumentException e) {
                    Log.w("NumberPickerTextColor", e);
                }
            }
        }
        return false;
    }

    public static String[] getPreviousDates(byte nDates) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");


        String prevDates[] = new String[nDates];
        prevDates[0] = sdf.format(last);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(last);


        for (byte i = 1; i < nDates; ++i) {
            calendar.add(Calendar.DAY_OF_YEAR, -1);

            prevDates[i] = sdf.format(calendar.getTime());
        }

        return prevDates;
    }

    private void fetchData() throws Exception {
        try {
            myTask = new BackgroundFetch();

            response = myTask.execute(new String[]{URL, NAMESPACE, methodName, soap_action, soap_username, soap_password, soap_licenceID}).get();
            parseXMLS(response);
            Toast.makeText(this, "Data updated", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Synchronization Error", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "Refresh later", Toast.LENGTH_SHORT).show();
            throw e;
        }
    }

    private void updateChart(LinearLayout swipe_layout, XYMultipleSeriesRenderer mRenderer, String valuta) {

        RangeCategorySeries series =
                new RangeCategorySeries("Exchange rate by day");
        int d = 1;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (Date date : values.keySet()) {
            double low = values.get(date).get(valuta)[0];
            double hig = values.get(date).get(valuta)[2];

            mRenderer.addXTextLabel(d++, sdf.format(date) + '.');
            series.add(low, hig);
            if (low < min)
                min = low;
            if (hig > max)
                max = hig;
        }

        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        dataset.addSeries(series.toXYSeries());

        XYSeriesRenderer renderer = new XYSeriesRenderer();
        renderer.setDisplayChartValues(true);
        renderer.setChartValuesTextSize(30);
        renderer.setChartValuesFormat(new java.text.DecimalFormat("#.####"));
        renderer.setColor(ContextCompat.getColor(this, R.color.colorAccent));


        mRenderer.setYAxisMax(max + 0.5);
        mRenderer.setYAxisMin(min - 0.5);
        mRenderer.setXAxisMin(0);
        mRenderer.setXAxisMax(d);

        mRenderer.removeAllRenderers();
        mRenderer.addSeriesRenderer(renderer);


        swipe_layout.removeViewAt(0);
        swipe_layout.addView(ChartFactory.getRangeBarChartView(
                this, dataset,
                mRenderer, BarChart.Type.DEFAULT), 0);
    }

    private void updateUI(LinearLayout swipe_layout, XYMultipleSeriesRenderer mRenderer, TextView datumText, Date last, NumberPicker np, NumberPicker np2, TextView textProdajni, TextView textSrednji, TextView textKupovni, EditText editText, EditText editText2, RadioGroup rg) {


        datumText.setText(sdf.format(last) + ".");
        np2.setMinValue(0); //from array first value
        np2.setMaxValue(currencies.length - 1); //to array last value

        np2.setDisplayedValues(currencies);
        setNumberPickerTextColor(np2, Color.WHITE);
        if (oldScrollValue == null)
            np2.setValue(1);
        else
            np2.setValue(oldScrollValue);

        np.setMinValue(0); //from array first value
        np.setMaxValue(2); //to array last value

        np.setDisplayedValues(samoRSD);
        setNumberPickerTextColor(np, Color.WHITE);
        np.setValue(1);


        editText.setText("100.0");
        editText2.setText(Double.toString(100 / values.get(last).get(currencies[np2.getValue()])[1]));

        textKupovni.setText(Double.toString(values.get(last).get(currencies[np2.getValue()])[0]));
        textSrednji.setText(Double.toString(values.get(last).get(currencies[np2.getValue()])[1]));
        textProdajni.setText(Double.toString(values.get(last).get(currencies[np2.getValue()])[2]));


        np2.setWrapSelectorWheel(true);
        np.setEnabled(false);
        editText.requestFocus();

        updateChart(swipe_layout, mRenderer, np2.getDisplayedValues()[np2.getValue()]);

    }

    private void parseXMLS(String response) {

        String[] splt = response.substring(1, response.length() - 1).split(", ");

        for (String s : splt)
            parseOneXML(s);
    }

    private void parseOneXML(String xml) {
        try {
            DocumentBuilderFactory dbf =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml));

            Document doc = db.parse(is);
            NodeList nodes = doc.getElementsByTagName("ExchangeRate");
            for (byte i = 0; i < nodes.getLength(); i++) {
                org.w3c.dom.Element element = (org.w3c.dom.Element) nodes.item(i);

                String xmlDate = element.getElementsByTagName("Date").item(0).getTextContent();
                String xmlCurr = element.getElementsByTagName("CurrencyCodeAlfaChar").item(0).getTextContent();
                String xmlKup = element.getElementsByTagName("BuyingRate").item(0).getTextContent();
                String xmlSre = element.getElementsByTagName("MiddleRate").item(0).getTextContent();
                String xmlProd = element.getElementsByTagName("SellingRate").item(0).getTextContent();

                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

                Double niz[] = {Double.parseDouble(xmlKup), Double.parseDouble(xmlSre), Double.parseDouble(xmlProd)};

                if (!values.containsKey(sdf.parse(xmlDate)))
                    values.put(sdf.parse(xmlDate), new TreeMap<String, Double[]>());

                values.get(sdf.parse(xmlDate)).put(xmlCurr, niz);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



