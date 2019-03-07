package io.github.kingschan1204.istock.module.spider.crawl.index;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.github.kingschan1204.istock.common.util.stock.StockDateUtil;
import io.github.kingschan1204.istock.common.util.stock.StockSpider;
import io.github.kingschan1204.istock.module.maindata.po.Stock;
import io.github.kingschan1204.istock.module.spider.AbstractSpider;
import io.github.kingschan1204.istock.module.spider.entity.WebPage;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * http://hq.sinajs.cn/list=%s
 * @author chenguoxiang
 * @create 2019-03-07 10:31
 **/
@Slf4j
public class SinaIndexSpider extends AbstractSpider<Stock>  {

    public SinaIndexSpider(String[] stockCode,ConcurrentLinkedQueue<Stock> queue){
        StringBuilder queryStr = new StringBuilder();
        for (String code : stockCode) {
            String resultCode = StockSpider.formatStockCode(code);
            if (null != resultCode) {
                queryStr.append(resultCode).append(",");
            }
        }
        String queryCode = queryStr.toString().replaceAll("\\,$", "");
        String pageUrl = String.format("http://hq.sinajs.cn/list=%s", queryCode);
        this.queue=queue;
        this.pageUrl=pageUrl;
        this.timeOut=8000;
        this.ignoreContentType=true;
        this.method= Connection.Method.GET;
    }

    @Override
    public void parsePage(WebPage webPage) throws Exception{
        String[] line = webPage.getDocument().text().split(";");
        JSONArray rows = new JSONArray();
        JSONObject json;
        for (String s : line) {
            String row = s.trim().replaceAll("^var\\D+|\"", "").replace("=", ",");
            String data[] = row.split(",");
            if (data.length < 30) {
                throw new Exception("代码不存在!");
            }
            double xj = StockSpider.mathFormat(data[4]);
            double zs = StockSpider.mathFormat(data[3]);
            double zf = (xj - zs) / zs * 100;
            double todayMax = StockSpider.mathFormat(data[5]);
            double todayMin = StockSpider.mathFormat(data[6]);
            json = new JSONObject();
            //一般这种是停牌的
            if (xj == 0) {
                //波动
                json.put("fluctuate", 0);
            } else {
                NumberFormat nf = NumberFormat.getNumberInstance();
                // 保留两位小数
                nf.setMaximumFractionDigits(2);
                // 如果不需要四舍五入，可以使用RoundingMode.DOWN
                nf.setRoundingMode(RoundingMode.UP);
                //波动
                json.put("fluctuate", StockSpider.mathFormat(nf.format(zf)));
            }
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
            Stock stock =new Stock();
            stock.setCode(data[0]);
            stock.setType(StockSpider.formatStockCode(data[0]).replaceAll("\\d", ""));
            stock.setName(data[1].replaceAll("\\s", ""));
            stock.setPrice(xj);
            stock.setTodayMax(todayMax);
            stock.setTodayMin(todayMin);
            stock.setYesterdayPrice(zs);
            stock.setPriceDate(StockDateUtil.getCurrentDateTimeNumber());
            queue.offer(stock);
        }
    }
}