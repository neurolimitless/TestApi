package hido.testapi;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button mButton;
    private Button mLeftButton;
    private Button mRightButton;
    private TextView mTextView;
    private ProgressBar mProgressBar;
    private ImageView mImageView;
    private List<Drawable> pictures = new ArrayList<>();
    private int index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButton = (Button) findViewById(R.id.load);
        mTextView = (TextView) findViewById(R.id.statusBar);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mLeftButton = (Button) findViewById(R.id.leftButton);
        mRightButton = (Button) findViewById(R.id.rightButton);
        mImageView = (ImageView) findViewById(R.id.mainView);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    switchProgressVisibility();
                    new PictureLoading().execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        mRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (index + 1 != pictures.size()) index++;
                else if (index + 1 == pictures.size()) index = 0;
                setPicture(index);
            }
        });
        mLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (index - 1 != -1) index--;
                else if (index - 1 == -1 && pictures.size() > 0) index = pictures.size() - 1;
                setPicture(index);
            }
        });
    }

    private void setPicture(int index) {
        if (pictures.size() != 0) mImageView.setImageDrawable(pictures.get(index));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setPictures(List<Drawable> pictures) {
        this.pictures = pictures;
    }


    private void switchProgressVisibility() {
        if (mProgressBar.getVisibility() == ProgressBar.VISIBLE)
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
        else mProgressBar.setVisibility(ProgressBar.VISIBLE);
    }

    private void setStatus(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextView.setText(status);
            }
        });
    }

    public class PictureLoading extends AsyncTask<String, Void, List<Drawable>> {

        private AlertDialog.Builder alertDialog;
        private String userId;

        private void showAlert(final String message) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    alertDialog.setMessage(message);
                    final AlertDialog dialog = alertDialog.create();
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                }
            });
        }

        @Override
        protected void onPreExecute() {
            alertDialog = new AlertDialog.Builder(MainActivity.this);
        }

        @Override
        protected List<Drawable> doInBackground(String... params) {
            try {
                //Login
                HttpURLConnection connection = (HttpURLConnection) new URL("http://testapi.us/api/login").openConnection();
                connection.setRequestMethod("POST");
                userId = getResponse(connection.getInputStream());
                connection.getInputStream().close();
                setStatus("User id: " + userId);
                //Get JSON
                connection = (HttpURLConnection) new URL("http://testapi.us/api/gallery/list").openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("user_id", userId);
                String json = getResponse(connection.getInputStream());
                connection.getInputStream().close();
                //Parse JSON
                JSONObject pictures = new JSONObject(json);
                int pictureCount = pictures.getJSONArray("images").length();
                List<Drawable> result = new ArrayList<>();
                setStatus("User id: " + userId + " , loaded " + result.size() + " / " + pictureCount + ".");
                for (int i = 0; i < pictureCount; i++) {
                    result.add(getImage(Integer.parseInt(pictures.getJSONArray("images").getJSONObject(i).getString("id"))));
                    setStatus("User id: " + userId + " , loaded " + result.size() + " / " + pictureCount + ".");
                }
                return result;
            } catch (Exception e) {
                showAlert("Network problems.");
                e.printStackTrace();
                return null;
            }
        }

        private Drawable getImage(int id) {
            try {
                URL url = new URL("http://testapi.us/api/gallery/" + id);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("user_id", userId);
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                bmOptions.inSampleSize = 32;
                InputStream stream = connection.getInputStream();
                Bitmap bi = BitmapFactory.decodeStream(stream, null, bmOptions);
                stream.close();
                double ratio = ((double) bi.getWidth() / (double) bi.getHeight());
                Bitmap bitmapResized = Bitmap.createScaledBitmap(bi, (int) (200 * ratio), 200, false);
                bi.recycle();
                return new BitmapDrawable(getResources(), bitmapResized);
            } catch (Exception e) {
                showAlert("Network problems.");
                e.printStackTrace();
                return null;
            }
        }

        private String getResponse(InputStream inputStream) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line = reader.readLine();
                StringBuilder builder = new StringBuilder();
                while (line != null) {
                    builder.append(line);
                    line = reader.readLine();
                }
                return builder.toString();
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            }
        }

        @Override
        protected void onPostExecute(List<Drawable> drawables) {
            if (drawables == null) showAlert("Network problems.");
            else if (drawables.size() != 0) {
                setPictures(drawables);
                switchProgressVisibility();
                mImageView.setImageDrawable(drawables.get(index));
            }
        }
    }
}
