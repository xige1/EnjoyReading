package com.example.zhangshihao.mytestapplication.novel;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.zhangshihao.mytestapplication.BaseActivity;
import com.example.zhangshihao.mytestapplication.R;
import com.example.zhangshihao.mytestapplication.novel.model.NovelOpenHelper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.Charset;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by zhangshihao on 2017/9/14.
 */
public class NovelReadActivity extends BaseActivity {

    private TextView tvSectionTitle;
    private TextView tvNovelBody;

    private String novelTitle;
    private String sectionUrl;
    private String baseUrl;
    private String novelName;
    private String sectionBody;
    private String preSectionUrl;
    private String nextSectionUrl;

    private Dialog loadingDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novel_read);
        loadingDialog = createLoadingDialog(this);
        loadingDialog.show();
        Intent readIntent = getIntent();
        if (readIntent != null) {
            //novelTitle = readIntent.getStringExtra("novel_title");
            sectionUrl = readIntent.getStringExtra("section_url");
            logw("sectionUrl = "+sectionUrl);
            baseUrl = sectionUrl.substring(0, sectionUrl.lastIndexOf("/") + 1);
        }
        init();
        showSectionContent();
    }

    private void showSectionContent() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Request request = new Request.Builder().url(sectionUrl).build();
                Call call = new OkHttpClient().newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        logd("okhttp error happened");
                        Looper.prepare();
                        showShortToast(NovelReadActivity.this, "网络错误，请重新再试");
                        Looper.loop();
                    }

                    @Override
                    //TODO : first section do gotoPreSection error
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            String sectionHtml = new String(response.body().bytes(), Charset.forName("GBK"));
                            Document doc = Jsoup.parse(sectionHtml);
                            Elements preNextSections = doc.select("div.box_con").select("div.bookname").select("div.bottem1").select("a[href]");
                            preSectionUrl = baseUrl.concat(preNextSections.get(1).attr("href"));
                            nextSectionUrl = baseUrl.concat(preNextSections.get(3).attr("href"));
                            logw("preSectionUrl = "+preSectionUrl+"\n nextSectionUrl = "+nextSectionUrl);
                            novelName = doc.select("div.con_top").select("a[href]").get(1).text();
                            logw("novelName = "+novelName);
                            novelTitle = doc.select("div.bookname").select("h1").first().text();
                            sectionBody = doc.select("div#content").first().html().replace("&nbsp;", "").replace("<br>", "");
                        } catch (Exception e) {
                            logd("no section error : " + e.getMessage());
                            Looper.prepare();
                            showShortToast(NovelReadActivity.this, "无此章节");
                            if(loadingDialog.isShowing()) {
                                loadingDialog.cancel();
                            }
                            Looper.loop();
                        }finally {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tvNovelBody.setText(sectionBody);
                                        tvSectionTitle.setText(novelTitle);
                                        logd("novelTitle = " + novelTitle);
                                        loadingDialog.cancel();
                                    }
                                });
                        }
                    }
                });
            }
        }).start();

    }

    private void init() {
        tvSectionTitle = (TextView) findViewById(R.id.tv_section_title);
        tvNovelBody = (TextView) findViewById(R.id.tv_novel_body);

        tvNovelBody.setMovementMethod(ScrollingMovementMethod.getInstance());//implement scroll effect
    }

    //handle buttons click events @{
    public void gotoCatalog(View view) {
        Intent sectionChooseIntent = new Intent(this,NovelSectionChooseActivity.class);
        sectionChooseIntent.putExtra("sections_url",baseUrl);
        startActivity(sectionChooseIntent);
        finish();
    }

    public void markNovel(View view) {
        try {
            NovelOpenHelper helper = NovelOpenHelper.getInstance(this);
            helper.insertData(novelName,novelTitle,sectionUrl);
            showShortToast(this,"书签添加成功，可于书签页查看");
            logd("click markNovel : \n novelName = "+novelName+
            "\n novelTitle = "+novelTitle+"\n sectionUrl = "+sectionUrl);
        }catch(Exception e){
            logd(e.getMessage());
            showShortToast(this,"书签添加失败");
        }
    }

    public void gotoPreSection(View view) {
        //处于第一章，点击上一章，跳转至目录界面
        if(preSectionUrl.contains("..")){
            Intent toCatalog = new Intent(this,NovelSectionChooseActivity.class);
            preSectionUrl = preSectionUrl.substring(0,preSectionUrl.lastIndexOf(".")-1);
            toCatalog.putExtra("sections_url",preSectionUrl);
            startActivity(toCatalog);
        }else {
            Intent newSection = new Intent(this, NovelReadActivity.class);
            newSection.putExtra("section_url", preSectionUrl);
            startActivity(newSection);
        }
        finish();
    }

    public void gotoNextSection(View view) {
        //处于最后一章，点击下一章，跳转至目录界面
        if(nextSectionUrl.contains("..")){
            Intent toCatalog = new Intent(this,NovelSectionChooseActivity.class);
            nextSectionUrl = nextSectionUrl.substring(0,nextSectionUrl.lastIndexOf(".")-1);
            toCatalog.putExtra("sections_url",nextSectionUrl);
            startActivity(toCatalog);
        }else{
            Intent newSection = new Intent(this, NovelReadActivity.class);
            newSection.putExtra("section_url", nextSectionUrl);
            startActivity(newSection);
        }
        finish();
    }
    //@}
}
