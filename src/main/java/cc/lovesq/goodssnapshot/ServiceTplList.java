package cc.lovesq.goodssnapshot;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.sun.jmx.mbeanserver.Util.cast;

@Component
public class ServiceTplList implements ServiceTplListInf {

    private static Log log = LogFactory.getLog(ServiceTplList.class);

    @Value(value="classpath:service.tpl")
    private Resource data;

    private List<ServiceTpl> serviceTplList = new ArrayList<>();

    private static Map<String, List<ServiceTpl>> serviceTplMap = new HashMap<>();

    private static Set<String> uniqueKeys = new HashSet<>();

    private WatchService watchService;

    @PostConstruct
    public void init() throws IOException {

        convertToList();
    }

    private void convertToList() {

        String json = getData();
        serviceTplList = JSONObject.parseArray(json, ServiceTpl.class);

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
        log.info("serviceTplMap: {}" + JSON.toJSONString(serviceTplMap));
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
                    String fileName = data.getFile().getName();
                    if (!fileName.equals(path.toString())) {
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

    public String getData(){
        try {
            ClassPathResource cpr = new ClassPathResource("service.tpl");
            byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
            return new String(bdata, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("IOException", e);
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
