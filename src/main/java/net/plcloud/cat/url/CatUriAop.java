package net.plcloud.cat.url;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.google.common.collect.Maps;
import net.plcloud.cat.TraceContext;
import net.plcloud.cat.util.CatAopUtils;
import net.plcloud.cat.util.CatUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@Aspect
@Component
@ConditionalOnClass(name = "org.springframework.web.bind.annotation.RequestMapping")
public class CatUriAop implements InitializingBean {

    /**
     * Logger for CatRestfullHandlerInterceptor
     */
//    private static final Logger logger = LoggerFactory.getLogger(CatUriAop.class);

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private RequestMappingInfoHandlerMapping requestMappingInfoHandlerMapping;

    private Map<HandlerMethod, RequestMappingInfo> requestMappingInfos = Maps.newLinkedHashMap();

    private Map<RequestMappingInfo, String> urlCache = Maps.newLinkedHashMap();

    private volatile boolean requestMappingInfosInit = false;

    public CatUriAop() {
    }

    @Around(value = "@annotation(org.springframework.web.bind.annotation.RequestMapping)||@annotation(org.springframework.web.bind.annotation.PostMapping)|| @annotation(org.springframework.web.bind.annotation.GetMapping)|| @annotation(org.springframework.web.bind.annotation.PutMapping)|| @annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object catPageURIRewrite(ProceedingJoinPoint pjp) throws Throwable {
        Transaction transaction = null;
        String name = "";
        try {
            String uri = null;
            if (!requestMappingInfosInit) {
                initHandlerMethods();
            }
            HandlerMethod handlerMethod = getHandlerMethod(pjp);
            RequestMappingInfo info = requestMappingInfos.get(handlerMethod);
            if (info != null) {
                uri = getUrl(info);
            }
            if (uri == null) {
                uri = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            }
            if (uri != null) {
                request.setAttribute("cat-page-uri", uri);
                transaction = CatUtil.initTransaction("URL", uri);
                name = handlerMethod.getBeanType().getName() + "." + handlerMethod.getMethod().getName();
                CatUtil.logEvent("URL.Method", name);
                //获取调用参数
                if (!"/error".equals(uri) && CatAopUtils.isPrintParams) {
                    CatUtil.logEvent("Args", name, "0", CatAopUtils.getParams(pjp));
                }
            }
        } catch (Exception e) {
            Cat.logEvent(CatUriAop.class.getName(), e.getMessage());
        }
        //2、调用核心逻辑
        Object retVal = null;
        try {
            TraceContext.init();
            retVal = CatAopUtils.handler(transaction, name, pjp);
            return retVal;
        }  finally {
            TraceContext.clean();
        }
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        initHandlerMethods();
    }

    private HandlerMethod getHandlerMethod(ProceedingJoinPoint pjp) {
        return new HandlerMethod(pjp.getThis(), ((MethodSignature) pjp.getSignature()).getMethod());
    }

    private String getUrl(RequestMappingInfo info) {
        String url = urlCache.get(info);
        if (url == null) {
            StringBuilder sb = new StringBuilder(request.getContextPath());
            if (info.getPatternsCondition() != null) {
                Set<String> patterns = info.getPatternsCondition().getPatterns();
                if (CollectionUtils.isNotEmpty(patterns)) {
                    sb.append(patterns.iterator().next());
                }
            }
//            List<String> params = Lists.newArrayList();
//            if (info.getMethodsCondition() != null
//                    && CollectionUtils.isNotEmpty(info.getMethodsCondition().getMethods())) {
//                params.add("method=" + info.getMethodsCondition());
//            }
//            if (info.getParamsCondition() != null
//                    && CollectionUtils.isNotEmpty(info.getParamsCondition().getExpressions())) {
//                params.add("params=" + info.getParamsCondition());
//            }
//            if (info.getHeadersCondition() != null
//                    && CollectionUtils.isNotEmpty(info.getHeadersCondition().getExpressions())) {
//                params.add("headers=" + info.getHeadersCondition());
//            }
//            if (info.getConsumesCondition() != null
//                    && CollectionUtils.isNotEmpty(info.getConsumesCondition().getExpressions())) {
//                params.add("consumes=" + info.getConsumesCondition());
//            }
//            if (info.getProducesCondition() != null
//                    && CollectionUtils.isNotEmpty(info.getProducesCondition().getExpressions())) {
//                params.add("produces=" + info.getProducesCondition());
//            }
//            if (info.getCustomCondition() != null) {
//                params.add("custom=" + info.getCustomCondition());
//            }
//            if (params.size() > 0) {
//                sb.append("-").append(Joiner.on(',').skipNulls().join(params));
//            }
            url = sb.toString();
            urlCache.put(info, url);
        }
        return url;
    }

    private void initHandlerMethods() {
        if (!requestMappingInfosInit) {
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingInfoHandlerMapping
                    .getHandlerMethods();
            if (MapUtils.isNotEmpty(handlerMethods)) {
                for (Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
                    requestMappingInfos.put(entry.getValue().createWithResolvedBean(), entry.getKey());
                }
                requestMappingInfosInit = true;
            }
        }
    }
}
