import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.util.EnumMap;

public class SoundManager {

    public enum SFX {
        USERNAME_ACCEPTED, MOVE, CAPTURE, CAPTURED, GAME_START, GAME_OVER,
        CHAT, YOUR_TURN, INVALID, CHALLENGE, KING_PROMOTION
    }

    private MediaPlayer musicPlayer;
    private boolean muted = false;
    private final EnumMap<SFX, AudioClip> clips = new EnumMap<>(SFX.class);

    public SoundManager() {
        // Preload all clips once at startup
        clips.put(SFX.USERNAME_ACCEPTED, loadClip("sfx_username_accepted.mp3"));
        clips.put(SFX.MOVE,       loadClip("sfx_move.mp3"));
        clips.put(SFX.CAPTURE,    loadClip("sfx_capture.mp3"));
        clips.put(SFX.CAPTURED,    loadClip("sfx_captured.mp3"));
        clips.put(SFX.GAME_START, loadClip("sfx_game_start.mp3"));
        clips.put(SFX.GAME_OVER,  loadClip("sfx_game_over.mp3"));
        clips.put(SFX.CHAT,       loadClip("sfx_chat.mp3"));
        clips.put(SFX.YOUR_TURN,  loadClip("sfx_your_turn.mp3"));
        clips.put(SFX.INVALID,    loadClip("sfx_invalid.mp3"));
        clips.put(SFX.CHALLENGE,  loadClip("sfx_challenge.mp3"));
        clips.put(SFX.KING_PROMOTION,  loadClip("sfx_king_promotion.mp3"));
    }

    private AudioClip loadClip(String filename) {
        try {
            String path = getClass().getResource("/sounds/" + filename).toExternalForm();
            return new AudioClip(path);
        } catch (Exception e) {
            System.out.println("[SoundManager] Missing: " + filename);
            return null;
        }
    }

    private MediaPlayer loadMusic(String filename) {
        try {
            String path = getClass().getResource("/sounds/" + filename).toExternalForm();
            Media media = new Media(path);
            MediaPlayer player = new MediaPlayer(media);
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setVolume(0.2);
            return player;
        } catch (Exception e) {
            System.out.println("[SoundManager] Missing music: " + filename);
            return null;
        }
    }

    public void playMusic(String filename) {
        stopMusic();
        musicPlayer = loadMusic(filename);
        if (musicPlayer != null && !muted) musicPlayer.play();
    }

    public void stopMusic() {
        if (musicPlayer != null) {
            musicPlayer.stop();
            musicPlayer.dispose();
            musicPlayer = null;
        }
    }

    public void playSFX(SFX sfx) {
        if (muted) return;
        AudioClip clip = clips.get(sfx);
        if (clip != null) clip.play();
    }

    public boolean isMuted() { return muted; }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (musicPlayer != null) {
            if (muted) musicPlayer.pause();
            else musicPlayer.play();
        }
    }

    public void toggleMute() { setMuted(!muted); }
}