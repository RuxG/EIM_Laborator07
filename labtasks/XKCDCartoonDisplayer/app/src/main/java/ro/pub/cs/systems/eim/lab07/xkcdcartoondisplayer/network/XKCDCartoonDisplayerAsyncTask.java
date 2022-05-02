package ro.pub.cs.systems.eim.lab07.xkcdcartoondisplayer.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.BasicResponseHandler;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import ro.pub.cs.systems.eim.lab07.xkcdcartoondisplayer.entities.XKCDCartoonInformation;
import ro.pub.cs.systems.eim.lab07.xkcdcartoondisplayer.general.Constants;

public class XKCDCartoonDisplayerAsyncTask extends AsyncTask<String, Void, XKCDCartoonInformation> {

    private TextView xkcdCartoonTitleTextView;
    private ImageView xkcdCartoonImageView;
    private TextView xkcdCartoonUrlTextView;
    private Button previousButton, nextButton;

    private class XKCDCartoonButtonClickListener implements Button.OnClickListener {

        private String xkcdComicUrl;

        public XKCDCartoonButtonClickListener(String xkcdComicUrl) {
            this.xkcdComicUrl = xkcdComicUrl;
        }

        @Override
        public void onClick(View view) {
            new XKCDCartoonDisplayerAsyncTask(xkcdCartoonTitleTextView, xkcdCartoonImageView, xkcdCartoonUrlTextView, previousButton, nextButton).execute(xkcdComicUrl);
        }

    }

    public XKCDCartoonDisplayerAsyncTask(TextView xkcdCartoonTitleTextView, ImageView xkcdCartoonImageView, TextView xkcdCartoonUrlTextView, Button previousButton, Button nextButton) {
        this.xkcdCartoonTitleTextView = xkcdCartoonTitleTextView;
        this.xkcdCartoonImageView = xkcdCartoonImageView;
        this.xkcdCartoonUrlTextView = xkcdCartoonUrlTextView;
        this.previousButton = previousButton;
        this.nextButton = nextButton;
    }

    @Override
    public XKCDCartoonInformation doInBackground(String... urls) {
        XKCDCartoonInformation xkcdCartoonInformation = new XKCDCartoonInformation();

        // TODO exercise 5a)
        // 1. obtain the content of the web page (whose Internet address is stored in urls[0])

        // - create an instance of a HttpClient object
        HttpClient httpClient = new DefaultHttpClient();

        // - create an instance of a HttpGet object
        HttpGet httpGet = new HttpGet(urls[0]);

        // - create an instance of a ResponseHandler object
        ResponseHandler<String> responseHandler = new BasicResponseHandler();

        // - execute the request, thus obtaining the web page source code
        String pageSourceCode;
        try {
            pageSourceCode = httpClient.execute(httpGet, responseHandler);
        } catch (IOException e) {
            e.printStackTrace();
            return xkcdCartoonInformation;
        }

        Document document = Jsoup.parse(pageSourceCode);
        Element htmlTag = document.child(0);

        // 2. parse the web page source code
        // - cartoon title: get the tag whose id equals "ctitle"
        Element divTagIdCtitle = htmlTag.getElementsByAttributeValue(Constants.ID_ATTRIBUTE, Constants.CTITLE_VALUE).first();
        xkcdCartoonInformation.setCartoonTitle(divTagIdCtitle.ownText());

        // - cartoon url
        //   * get the first tag whose id equals "comic"
        Element divTagIdComic = htmlTag.getElementsByAttributeValue(Constants.ID_ATTRIBUTE, Constants.COMIC_VALUE).first();

        //   * get the embedded <img> tag
        String cartoonInternetAddress = divTagIdComic.getElementsByTag(Constants.IMG_TAG).attr(Constants.SRC_ATTRIBUTE);

        //   * get the value of the attribute "src"
        //   * prepend the protocol: "http:"
        String uri = "http:".concat(cartoonInternetAddress);

        // - cartoon bitmap (only if using Apache HTTP Components)
        //   * create the HttpGet object
        httpGet.setURI(URI.create(uri));

        //   * execute the request and obtain the HttpResponse object
        HttpResponse httpResponse;
        try {
            httpResponse = httpClient.execute(httpGet);
        } catch (IOException e) {
            e.printStackTrace();
            return xkcdCartoonInformation;
        }

        //   * get the HttpEntity object from the response
        //   * get the bitmap from the HttpEntity stream (obtained by getContent()) using Bitmap.decodeStream() method
        HttpEntity entity = httpResponse.getEntity();
        if (entity == null) {
            return xkcdCartoonInformation;
        }
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(entity.getContent());
        } catch (IOException e) {
            e.printStackTrace();
            return xkcdCartoonInformation;
        }
        xkcdCartoonInformation.setCartoonBitmap(bitmap);

        // - previous cartoon address
        //   * get the first tag whole rel attribute equals "prev"
        Element divTagIdPrev = htmlTag.getElementsByAttributeValue(Constants.REL_ATTRIBUTE, Constants.PREVIOUS_VALUE).first();
        //   * get the href attribute of the tag
        String href = divTagIdPrev.attr(Constants.HREF_ATTRIBUTE);
        //   * prepend the value with the base url: http://www.xkcd.com
        uri = Constants.XKCD_INTERNET_ADDRESS.concat(href);

        xkcdCartoonInformation.setPreviousCartoonUrl(uri);

        // - next cartoon address
        //   * get the first tag whole rel attribute equals "next"
        Element divTagIdNext = htmlTag.getElementsByAttributeValue(Constants.REL_ATTRIBUTE, Constants.NEXT_VALUE).first();

        //   * get the href attribute of the tag
        href = divTagIdNext.attr(Constants.HREF_ATTRIBUTE);

        //   * prepend the value with the base url: http://www.xkcd.com
        uri = Constants.XKCD_INTERNET_ADDRESS.concat(href);

        xkcdCartoonInformation.setNextCartoonUrl(uri);

        return  xkcdCartoonInformation;
    }

    @Override
    protected void onPostExecute(final XKCDCartoonInformation xkcdCartoonInformation) {

        // TODO exercise 5b)
        // map each member of xkcdCartoonInformation object to the corresponding widget

        // cartoonTitle -> xkcdCartoonTitleTextView
        xkcdCartoonTitleTextView.setText(xkcdCartoonInformation.getCartoonTitle());

        // cartoonBitmap -> xkcdCartoonImageView (only if using Apache HTTP Components)
        xkcdCartoonImageView.setImageBitmap(xkcdCartoonInformation.getCartoonBitmap());

        // cartoonUrl -> xkcdCartoonUrlTextView
       // xkcdCartoonUrlTextView.setText(xkcdCartoonInformation.getCartoonUrl());

        // based on cartoonUrl fetch the bitmap
        // and put it into xkcdCartoonImageView

        // previousCartoonUrl, nextCartoonUrl -> set the XKCDCartoonUrlButtonClickListener for previousButton, nextButton
        previousButton.setOnClickListener(new XKCDCartoonButtonClickListener(xkcdCartoonInformation.getPreviousCartoonUrl()));
        nextButton.setOnClickListener(new XKCDCartoonButtonClickListener(xkcdCartoonInformation.getNextCartoonUrl()));

    }

}
