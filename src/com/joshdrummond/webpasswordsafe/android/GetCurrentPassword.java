/*
    Copyright 2010 Josh Drummond

    This file is part of WebPasswordSafe-Android.

    WebPasswordSafe-Android is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    WebPasswordSafe-Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with WebPasswordSafe-Android; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package com.joshdrummond.webpasswordsafe.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.Arrays;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import com.joshdrummond.webpasswordsafe.android.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


/**
 * Invokes the GetCurrentPassword web service from a WebPasswordSafe instance
 * 
 * @author Josh Drummond
 *
 */
public class GetCurrentPassword extends Activity
{
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Button btn = (Button)findViewById(R.id.submitButton);
        btn.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                doSubmit();
            }
        });
    }
    
    private void doSubmit()
    {
        TextView status = (TextView)findViewById(R.id.status);
        String url = ((EditText)findViewById(R.id.url)).getText().toString();
        String authnUsername = ((EditText)findViewById(R.id.authnUsername)).getText().toString();
        String authnPassword = ((EditText)findViewById(R.id.authnPassword)).getText().toString();
        String passwordName = ((EditText)findViewById(R.id.passwordName)).getText().toString();
        
        final String requestSOAP = new StringBuffer().append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wps=\"http://www.joshdrummond.com/webpasswordsafe/schemas\">").
            append("<soapenv:Header/>").
            append("<soapenv:Body>").
            append("<wps:GetCurrentPasswordRequest>").
            append("<wps:authnUsername>").append(authnUsername).append("</wps:authnUsername>").
            append("<wps:authnPassword>").append(authnPassword).append("</wps:authnPassword>").
            append("<wps:passwordName>").append(passwordName).append("</wps:passwordName>").
            append("</wps:GetCurrentPasswordRequest>").
            append("</soapenv:Body>").
            append("</soapenv:Envelope>").toString();
        try
        {
            HttpClient httpClient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();
            ContentProducer cp = new ContentProducer() {
                public void writeTo(OutputStream outstream) throws IOException {
                    Writer writer = new OutputStreamWriter(outstream, "UTF-8");
                    writer.write(requestSOAP);
                    writer.flush();
                }
            };
            HttpEntity entity = new EntityTemplate(cp);
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(entity);
            HttpResponse httpResponse = httpClient.execute(httpPost, localContext);
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            StringBuffer responseSOAP = new StringBuffer();
            String line = reader.readLine();
            while (line != null)
            {
                responseSOAP.append(line);
                line = reader.readLine();
            }
            status.setText(parseResponse(responseSOAP.toString()));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            status.setText("ERROR: "+e.getMessage());
        }
    }
    
    private String parseResponse(String responseSOAP)
    {
        String response = "";
        try
        {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            GetCurrentPasswordHandler handler = new GetCurrentPasswordHandler();
            xr.setContentHandler(handler);
            xr.parse(new InputSource(new StringReader(responseSOAP)));
            response = handler.getParsedData();
        }
        catch (Exception e)
        {
            response = "ERROR parsing: "+Arrays.deepToString(e.getStackTrace());
        }
        return response;
    }
    
    class GetCurrentPasswordHandler extends DefaultHandler
    {
        private String tag = "";
        private String success = "";
        private String message = "";
        private String password = "";
        
        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException
        {
            tag = localName;
        }
        
        @Override
        public void characters(char ch[], int start, int length)
        {
            String text = new String(ch, start, length);
            if (tag.equals("success"))
            {
                success = text;
            }
            else if (tag.equals("message"))
            {
                message = text;
            }
            else if (tag.equals("password"))
            {
                password = text;
            }
        }
        
        public String getParsedData()
        {
            return "Password="+password+"\nSuccess="+success+"\nMessage="+message;
        }
        
    }
}