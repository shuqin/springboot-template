package cc.lovesq.goodssnapshot;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.sun.jmx.mbeanserver.Util.cast;

@Component
public class ServiceTplSplitList implements ServiceTplListInf {

    @Value(value="classpath:service1.tpl")
    private Resource data1;

    @Value(value="classpath:service2.tpl")
    private Resource data2;

    private List<ServiceTpl> serviceTplList = new ArrayList<>();

    private static Map<String, List<ServiceTpl>> serviceTplMap = new HashMap<>();

    private static Set<String> uniqueKeys = new HashSet<>();

    private WatchService watchService;

    @PostConstruct
    public void init() throws IOException {

        convertToList();

        watchService = FileSystems.getDefault().newWatchService();
        System.out.println("parent: " + data1.getFile().getParent());
        Paths.get(data1.getFile().getParent()).register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);
        Paths.get(data2.getFile().getParent()).register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);

        new Thread(() -> listenFileModified()).start();
    }

    private void convertToList() {

        try {
            serviceTplList.clear();
            String json = getData(data1);
            serviceTplList.addAll(JSONObject.parseArray(json, ServiceTpl.class));

            String json2 = getData(data2);
            serviceTplList.addAll(JSONObject.parseArray(json2, ServiceTpl.class));
        } catch (Exception ex) {
            serviceTplList.add(new ServiceTpl("express", "快递发货", "支持快递发货，本商品$info"));
            serviceTplList.add(new ServiceTpl("selfetch", "到店自提", "可就近选择自提点并预约自提时间"));
            serviceTplList.add(new ServiceTpl("localDelivery", "同城配送", "可选择同城配送并预约送达时间，本商品运费：最低 ￥$startPrice 元，$deliveryPrice"));
            serviceTplList.add(new ServiceTpl("codpay", "货到付款", "此商品支持货到付款"));

            serviceTplList.add(new ServiceTpl("refundAndReturn", "支持退换","支持买家申请退换"));
            serviceTplList.add(new ServiceTpl("secureService", "收货后结算", "该店铺交易由有赞提供资金存管服务，当符合以下条件时，资金自动结算给商家：买家确认收货或到达约定的自动确认收货日期。交易资金未经有赞存管的情形（储值型、电子卡券等）不在本服务范围内。"));
            serviceTplList.add(new ServiceTpl("retailShop", "线下门店", "该店铺拥有线下门店，商家已展示门店信息"));
        }


        Map<String, List<ServiceTpl>> serviceTplLocalMap = new HashMap<>();
        for (ServiceTpl serviceTpl : serviceTplList) {
            String key = serviceTpl.getKey();
            String uniqueKey = serviceTpl.getUniqueKey();
            if (!serviceTplLocalMap.containsKey(key)) {
                serviceTplLocalMap.put(key, new ArrayList<>());
                uniqueKeys.add(uniqueKey);
            }
            serviceTplLocalMap.get(key).add(serviceTpl);
        }
        serviceTplMap = serviceTplLocalMap;
    }

    private void downgrade() {

    }

    private void listenFileModified() {
        try {
            while(true) {
                WatchKey key = watchService.poll(20, TimeUnit.SECONDS);
                if (key == null) {
                    continue;
                }
                //获取监听事件
                for (WatchEvent<?> event : key.pollEvents()) {
                    //获取监听事件类型
                    WatchEvent.Kind kind = event.kind();
                    //异常事件跳过
                    if (kind != StandardWatchEventKinds.ENTRY_MODIFY) {
                         continue;
                    }
                    //获取监听Path
                    Path path = cast(event.context());
                    //只关注目标文件
                    String fileName1 = data1.getFile().getName();
                    String fileName2 = data2.getFile().getName();
                    if (!fileName1.equals(path.toString()) && !  fileName2.equals(path.toString())) {
                        continue;
                    }
                    convertToList();

                }
                //处理监听key后(即处理监听事件后)，监听key需要复位，便于下次监听
                key.reset();
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getCause());
        }

    }

    public boolean containsKey(String key) {
        return serviceTplMap.containsKey(key);
    }

    public ServiceTpl getTpl(String key, long timestamp) {
        List<ServiceTpl> serviceTpls = serviceTplMap.get(key);
        if (CollectionUtils.isEmpty(serviceTpls)) {
            return null;
        }

        for (ServiceTpl serviceTpl: serviceTpls) {
            if (serviceTpl.getStart() <= timestamp && serviceTpl.getEnd() > timestamp) {
                return serviceTpl;
            }
        }
        return null;
    }

    public String getData(Resource data){
        try {
            File file = data.getFile();
            String jsonData = this.jsonRead(file);
            return jsonData;
        } catch (Exception e) {
            return null;
        }
    }

    private String jsonRead(File file){
        Scanner scanner = null;
        StringBuilder buffer = new StringBuilder();
        try {
            scanner = new Scanner(file, "utf-8");
            while (scanner.hasNextLine()) {
                buffer.append(scanner.nextLine());
            }
        } catch (Exception e) {

        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        return buffer.toString();
    }
}
