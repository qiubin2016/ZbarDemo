/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lee.zbardemo;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;

/**
 * Manages beeps and vibrations for {@link }.
 */
public final class BeepManager implements MediaPlayer.OnErrorListener, Closeable, MediaPlayer.OnCompletionListener {

  private static final String TAG = BeepManager.class.getSimpleName();

  private static final float BEEP_VOLUME = 0.10f;
  private static final long VIBRATE_DURATION = 200L;

  private final Activity activity;
  private MediaPlayer mediaPlayer;
  private boolean playBeep;
  private boolean vibrate;

  public BeepManager(Activity activity) {
    this.activity = activity;
    this.mediaPlayer = null;
    update();
  }

  public synchronized void update() {
    playBeep = shouldBeep(activity);
    vibrate = true;
    if (playBeep && mediaPlayer == null) {
      // The volume on STREAM_SYSTEM is not adjustable, and users found it too loud,
      // so we now play on the music stream.
      activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
      mediaPlayer = buildMediaPlayer(activity);
    }
  }

  public synchronized void playBeepSoundAndVibrate() {
    Log.i(TAG, "-------1---");
    if (playBeep && mediaPlayer != null) {
      Log.i(TAG, "-------10---");
//      mediaPlayer.stop();
      mediaPlayer.seekTo(0);
//      if (mediaPlayer.isPlaying())  //上次还未播放完，从头开始播放
//      {Log.i(TAG, "-------11---");
//        mediaPlayer.seekTo(0);
//      }
      Log.i(TAG, "-------14---");
      mediaPlayer.start();
    }
    if (vibrate) {
      Log.i(TAG, "-------12---");
      //不允许振动
      Vibrator vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
      vibrator.vibrate(VIBRATE_DURATION);
    }
    Log.i(TAG, "-------13---");
  }

  private static boolean shouldBeep(Context activity) {
    boolean shouldPlayBeep = true;
    // See if sound settings overrides this
    AudioManager audioService = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
    if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
      shouldPlayBeep = false;
    }
    return shouldPlayBeep;
  }

  private MediaPlayer buildMediaPlayer(Context activity) {
    MediaPlayer mediaPlayer = new MediaPlayer();
    try (AssetFileDescriptor file = activity.getResources().openRawResourceFd(R.raw.beep)) {
      mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
      mediaPlayer.setOnErrorListener(this);
      mediaPlayer.setOnCompletionListener(this);    //增加onCompletion监听
      mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
      mediaPlayer.setLooping(false);
      mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
      mediaPlayer.prepare();
      return mediaPlayer;
    } catch (IOException ioe) {
      Log.w(TAG, ioe);
      mediaPlayer.release();
      return null;
    }
  }

  @Override
  public synchronized boolean onError(MediaPlayer mp, int what, int extra) {
    if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
      Log.i(TAG, "onError 1");
      // we are finished, so put up an appropriate error toast if required and finish
      activity.finish();
    } else {
      Log.i(TAG, "onError 2");
      // possibly media player error, so release and recreate
      close();
      update();
    }
    return true;
  }

  @Override
  public synchronized void close() {
    if (mediaPlayer != null) {
      mediaPlayer.release();
      mediaPlayer = null;
    }
  }

  @Override
  public void onCompletion(MediaPlayer mp) {
    Log.i(TAG, "onCompletion");
  }
}
