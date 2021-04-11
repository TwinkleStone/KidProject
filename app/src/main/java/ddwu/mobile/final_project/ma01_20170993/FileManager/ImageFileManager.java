package ddwu.mobile.final_project.ma01_20170993.FileManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageFileManager {

    final static String TAG = "ImageFileManager";
    final static String IMG_EXT = ".jpg";

    private Context context;

    public ImageFileManager(Context context) {
        this.context = context;
    }

    /* Bitmap 과 Bitmap 다운로드에 사용한 URL 을 전달받아 내부저장소에 JPG 파일로 저장 후
    파일 이름을 반환, 파일 저장 실패 시 null 반환 */
    public String saveBitmapToInternal(Bitmap bitmap) {
        String fileName = null;
        try {
            fileName = getCurrentTime() + IMG_EXT;

            // 내부 저장소에 파일 생성
            File saveFile = new File(context.getFilesDir(), fileName);
            // file용 output 스트림 생성
            FileOutputStream fos = new FileOutputStream(saveFile);

            // 비트맵 이미지를 파일에 기록, Bitmap의 크기가 클 경우 이 부분에서 조정
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            fos.flush();
            fos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fileName = null;
        } catch (IOException e) {
            e.printStackTrace();
            fileName = null;
        }
        return fileName;
    }

    /* 현재 시간(초단위)을 문자열로 단위   */
    private String getCurrentTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss"); //SSS가 밀리세컨드 표시
        return dateFormat.format(new Date());
    }

    /* 이미지 다운로드에 사용하는 url 을 전달받아
     url 에 해당하는 내부 저장소에 이미지 파일이 있는지 확인 후 있으면 Bitmap 반환, 없을 경우 null 반환 */
    public Bitmap getSavedBitmapFromInternal(String url) {
        String fileName = getFileNameFromUrl(url);
        String path = context.getFilesDir().getPath() + "/" + fileName;

        // 비트맵일 경우의 읽기
        Bitmap bitmap = BitmapFactory.decodeFile(path);

        Log.i(TAG, path);

        return bitmap;
    }

    public String getSavedPathFromInternal(String url) {
        String fileName = getFileNameFromUrl(url);
        String path = context.getFilesDir().getPath() + "/" + fileName;
        return path;
    }

    /* URL에서 파일명에 해당하는 부분을 추출 (예: http://www.dongduk.ac.kr/main/main.jpg → main.jpg)
     사용하는 URL에 따라 달라질 수 있으므로 사용 시 확인 필요 */
    public String getFileNameFromUrl(String url) {
        String fileName = Uri.parse(url).getLastPathSegment();
        return fileName.replace("\n", "");
    }

    /* Bitmap 과 Bitmap 다운로드에 사용한 URL 을 전달받아 내부저장소에 JPG 파일로 저장 후
    파일 이름을 반환, 파일 저장 실패 시 null 반환 */
    public String saveBitmapToInternal(Bitmap bitmap, String url) {
        String fileName = null;
        try {
            fileName = getFileNameFromUrl(url);

            // 내부 저장소에 파일 생성
            File saveFile = new File(context.getFilesDir(), fileName);
            // file용 output 스트림 생성
            FileOutputStream fos = new FileOutputStream(saveFile);

            // 비트맵 이미지를 파일에 기록, Bitmap의 크기가 클 경우 이 부분에서 조정
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            fos.flush();
            fos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fileName = null;
        } catch (IOException e) {
            e.printStackTrace();
            fileName = null;
        }
        return fileName;
    }
}
