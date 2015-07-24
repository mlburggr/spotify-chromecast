package com.chromecastspotify.chromecastspotify;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.net.Uri;
import android.widget.TextView;
import android.widget.Button;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.mediarouter.*;
import android.support.v7.media.MediaRouter.Callback;
import android.support.v7.media.MediaRouter.RouteInfo;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;



import com.spotify.sdk.android.player.*;
import com.spotify.sdk.android.authentication.*;

import java.io.IOException;


public class MainActivity extends ActionBarActivity implements PlayerNotificationCallback, ConnectionStateCallback{

    private Button mButton;

    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter.Callback mMediaRouterCallback;
    private CastDevice mSelectedDevice;
    private GoogleApiClient mApiClient;
    private RemoteMediaPlayer mRemoteMediaPlayer;
    private Cast.Listener mCastClientListener;
    private boolean mWaitingForReconnect = false;
    private boolean mApplicationStarted = false;
    private boolean mVideoIsLoaded;
    private boolean mIsPlaying;

    // TODO: Replace with your client ID
    private static final String CLIENT_ID = "0c6c95b5110a4aeeabf1388c2f904218";
    // TODO: Replace with your redirect URI
    private static final String REDIRECT_URI = "ccspotify://callback";

    private Player mPlayer;

    private static final int REQUEST_CODE = 1337;
    //Spotify Functions

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("MainActivity", "Playback event received: " + eventType.name());
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.d("MainActivity", "Playback error received: " + errorType.name());
    }


//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
//        super.onActivityResult(requestCode, resultCode, intent);
//
//        // Check if result comes from the correct activity
//        if (requestCode == REQUEST_CODE) {
//            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
//            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
//                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
//                mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
//                    @Override
//                    public void onInitialized(Player player) {
//                        mPlayer.addConnectionStateCallback(MainActivity.this);
//                        mPlayer.addPlayerNotificationCallback(MainActivity.this);
//                        mPlayer.play("spotify:track:2TpxZ7JUBn3uw46aR7qd6V");
//                    }
//
//                    @Override
//                    public void onError(Throwable throwable) {
//                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
//                    }
//                });
//            }
//        }
//
//    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("onNewIntent","1");
        Uri uri = intent.getData();
        intent.getData();
        Log.d("onNewIntent", "uri: " + uri);
        if (uri != null) {
            AuthenticationResponse response = AuthenticationResponse.fromUri(uri);
            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    String token = response.getAccessToken();
                    Log.d("onNewIntent", "2");


                    Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                    mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                        @Override
                        public void onInitialized(Player player) {
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                        }
                    });

                    startMusic();

                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    Log.d("onNewIntent","error");
                    break;

                // Most likely auth flow was cancelled
                default:
                    Log.d("onNewIntent","3");
                    // Handle other cases
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start media router discovery
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initMediaRouter();

        Log.d("onCreate", "1");

        final AuthenticationRequest request = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI)
                .setScopes(new String[]{"user-read-private", "streaming"})
                .build();

        Log.d("onCreate", "2");

        AuthenticationClient.openLoginInBrowser(this, request);

        Log.d("onCreate", "3");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onCreateOptionsMenu( menu );
        getMenuInflater().inflate( R.menu.menu_main, menu );
        MenuItem mediaRouteMenuItem = menu.findItem( R.id.media_route_menu_item );
        MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        return true;
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }


    private void initMediaRouter() {
        // Configure Cast device discovery
        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(
                        CastMediaControlIntent.categoryForCast( getString( R.string.app_id ) ) )
                .build();
        mMediaRouterCallback = new MediaRouterCallback();
    }

    private void initCastClientListener() {
        mCastClientListener = new Cast.Listener() {
            @Override
            public void onApplicationStatusChanged() {
            }

            @Override
            public void onVolumeChanged() {
            }

            @Override
            public void onApplicationDisconnected( int statusCode ) {
                teardown();
            }
        };
    }

    private void initRemoteMediaPlayer() {
        mRemoteMediaPlayer = new RemoteMediaPlayer();
        mRemoteMediaPlayer.setOnStatusUpdatedListener( new RemoteMediaPlayer.OnStatusUpdatedListener() {
            @Override
            public void onStatusUpdated() {
                MediaStatus mediaStatus = mRemoteMediaPlayer.getMediaStatus();
                mIsPlaying = mediaStatus.getPlayerState() == MediaStatus.PLAYER_STATE_PLAYING;
            }
        });

        mRemoteMediaPlayer.setOnMetadataUpdatedListener( new RemoteMediaPlayer.OnMetadataUpdatedListener() {
            @Override
            public void onMetadataUpdated() {
            }
        });
    }

    private void control() {
        if( mRemoteMediaPlayer == null || !mVideoIsLoaded )
            return;

        if( mIsPlaying ) {
            mRemoteMediaPlayer.pause(mApiClient);
            mButton.setText( getString( R.string.resume_video ) );
        } else {
            mRemoteMediaPlayer.play( mApiClient );
            mButton.setText( getString( R.string.pause_video ) );
        }
    }

    private void startVideo() {
        MediaMetadata mediaMetadata = new MediaMetadata( MediaMetadata.MEDIA_TYPE_MOVIE );
        mediaMetadata.putString( MediaMetadata.KEY_TITLE, getString( R.string.video_title ) );

        MediaInfo mediaInfo = new MediaInfo.Builder( getString( R.string.video_url ) )
                .setContentType( getString( R.string.content_type_mp4 ) )
                .setStreamType( MediaInfo.STREAM_TYPE_BUFFERED )
                .setMetadata( mediaMetadata )
                .build();
        try {
            mRemoteMediaPlayer.load( mApiClient, mediaInfo, true )
                    .setResultCallback( new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                        @Override
                        public void onResult( RemoteMediaPlayer.MediaChannelResult mediaChannelResult ) {
                            if( mediaChannelResult.getStatus().isSuccess() ) {
                                mVideoIsLoaded = true;
                                mButton.setText( getString( R.string.pause_video ) );
                            }
                        }
                    } );
        } catch( Exception e ) {
        }
    }

    private void startMusic() {

                            mPlayer.addConnectionStateCallback(MainActivity.this);
                            mPlayer.addPlayerNotificationCallback(MainActivity.this);
                            mPlayer.play("spotify:track:2TpxZ7JUBn3uw46aR7qd6V");

    }


    @Override
    protected void onPause() {
        if ( isFinishing() ) {
            // End media router discovery
            mMediaRouter.removeCallback( mMediaRouterCallback );
        }
        super.onPause();
    }

    private class MediaRouterCallback extends MediaRouter.Callback {

        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo info) {
            initCastClientListener();
            initRemoteMediaPlayer();

            mSelectedDevice = CastDevice.getFromBundle( info.getExtras() );

            launchReceiver();
        }

        @Override
        public void onRouteUnselected( MediaRouter router, RouteInfo info ) {
            teardown();
            mSelectedDevice = null;
            mButton.setText( getString( R.string.play_video ) );
            mVideoIsLoaded = false;
        }
    }

    private void launchReceiver() {
        Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                .builder( mSelectedDevice, mCastClientListener );

        ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks();
        ConnectionFailedListener mConnectionFailedListener = new ConnectionFailedListener();
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Cast.API, apiOptionsBuilder.build() )
                .addConnectionCallbacks( mConnectionCallbacks )
                .addOnConnectionFailedListener( mConnectionFailedListener )
                .build();

        mApiClient.connect();
    }

    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {

        @Override
        public void onConnected( Bundle hint ) {
            if( mWaitingForReconnect ) {
                mWaitingForReconnect = false;
                reconnectChannels( hint );
            } else {
                try {
                    Cast.CastApi.launchApplication( mApiClient, getString( R.string.app_id ), false )
                            .setResultCallback( new ResultCallback<Cast.ApplicationConnectionResult>() {
                                                    @Override
                                                    public void onResult(Cast.ApplicationConnectionResult applicationConnectionResult) {
                                                        Status status = applicationConnectionResult.getStatus();
                                                        if( status.isSuccess() ) {
                                                            //Values that can be useful for storing/logic
                                                            ApplicationMetadata applicationMetadata = applicationConnectionResult.getApplicationMetadata();
                                                            String sessionId = applicationConnectionResult.getSessionId();
                                                            String applicationStatus = applicationConnectionResult.getApplicationStatus();
                                                            boolean wasLaunched = applicationConnectionResult.getWasLaunched();

                                                            mApplicationStarted = true;
                                                            reconnectChannels( null );
                                                        }
                                                    }
                                                }
                            );
                } catch ( Exception e ) {

                }
            }
        }

        @Override
        public void onConnectionSuspended(int i) {
            mWaitingForReconnect = true;
        }
    }

    private void reconnectChannels( Bundle hint ) {
        if( ( hint != null ) && hint.getBoolean( Cast.EXTRA_APP_NO_LONGER_RUNNING ) ) {
            //Log.e( TAG, "App is no longer running" );
            teardown();
        } else {
            try {
                Cast.CastApi.setMessageReceivedCallbacks( mApiClient, mRemoteMediaPlayer.getNamespace(), mRemoteMediaPlayer );
            } catch( IOException e ) {
                //Log.e( TAG, "Exception while creating media channel ", e );
            } catch( NullPointerException e ) {
                //Log.e( TAG, "Something wasn't reinitialized for reconnectChannels" );
            }
        }
    }

    private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed( ConnectionResult connectionResult ) {
            teardown();
        }
    }




    private void teardown() {
        if( mApiClient != null ) {
            if( mApplicationStarted ) {
                try {
                    Cast.CastApi.stopApplication( mApiClient );
                    if( mRemoteMediaPlayer != null ) {
                        Cast.CastApi.removeMessageReceivedCallbacks( mApiClient, mRemoteMediaPlayer.getNamespace() );
                        mRemoteMediaPlayer = null;
                    }
                } catch( IOException e ) {
                    //Log.e( TAG, "Exception while removing application " + e );
                }
                mApplicationStarted = false;
            }
            if( mApiClient.isConnected() )
                mApiClient.disconnect();
            mApiClient = null;
        }
        mSelectedDevice = null;
        mVideoIsLoaded = false;
    }

}

