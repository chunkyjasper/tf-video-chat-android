package com.example.jasper.depressionchat;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class APIRequest extends Request<JSONArray>{
    private final String CHARSET = "utf-8";
    private final Listener<JSONArray> mlistener;
    private final String API_ROOT = "http://192.168.1.171:8000/";
    private final String OAUTH_ID = Constants.OAUTH_ID;
    private final String OAUTH_SECRET = Constants.OAUTH_SECRET;
    private JSONObject mRequestBody;
    public enum ContentType {JSON, FORM }
    private ContentType contentType;
    private String token;
    private boolean isarray = false;



    // GET with token
    public APIRequest(String url, String token,
                      Listener<JSONArray> listener, ErrorListener errorListener) {
        super(Method.GET, Constants.DOMAIN.concat(url), errorListener);
        this.mlistener = listener;
        this.token = token;
    }

    // POST with token
    public APIRequest(String url, JSONObject mRequestBody, ContentType contentType, String token,
                      Listener<JSONArray> listener, ErrorListener errorListener) {
        super(Method.POST, Constants.DOMAIN.concat(url), errorListener);
        this.mlistener = listener;
        this.token = token;
        this.mRequestBody = mRequestBody;
        this.contentType = contentType;
    }
    // POST without token for requesting token
    public APIRequest(String url, JSONObject mRequestBody, ContentType contentType,
                      Listener<JSONArray> listener, ErrorListener errorListener) {
        super(Method.POST, Constants.DOMAIN.concat(url), errorListener);
        this.mlistener = listener;
        this.mRequestBody = mRequestBody;
        this.contentType = contentType;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<>();
        if (token != null) {
            headers.put("Authorization", "Bearer ".concat(token));
        }
        return headers;
    }

    @Override
    protected void deliverResponse(JSONArray response) {
            mlistener.onResponse(response);
    }

    @Override
    protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
        String jsonString;
        try {
            jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers, CHARSET));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        }
        try {
            return Response.success(new JSONArray(jsonString),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (JSONException je) {
            // JSONArray with single entry
            try {
                JSONArray jsonArray = new JSONArray();
                return Response.success(jsonArray.put(new JSONObject(jsonString)),
                        HttpHeaderParser.parseCacheHeaders(response));
            } catch (JSONException je2) {
                return Response.error(new ParseError(je2));
            }
        }
    }

    @Override
    public Map<String,String> getParams() {
        Map<String, String> params = new HashMap<>();
        try {
            Iterator<String> nameItr = mRequestBody.keys();
            while (nameItr.hasNext()) {
                String name = nameItr.next();
                params.put(name, mRequestBody.getString(name));
            }
        } catch(JSONException e){
            return null;
        }
        return params;
    }


    @Override
    public String getBodyContentType() {
        String contentTypeStr = "";
        switch (contentType) {
            case JSON:
                contentTypeStr = String.format("application/json; charset=%s", CHARSET);
                break;

            case FORM:
                contentTypeStr = String.format("application/x-www-form-urlencoded; charset=%s", CHARSET);
        }


        return contentTypeStr;
    }

}
