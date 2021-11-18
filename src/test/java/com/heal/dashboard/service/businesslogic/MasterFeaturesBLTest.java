package com.heal.dashboard.service.businesslogic;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;
import com.heal.dashboard.service.beans.MasterFeaturesBean;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.dao.mysql.FeaturesDao;

@RunWith(SpringRunner.class)
public class MasterFeaturesBLTest {


    @InjectMocks
   private MasterFeaturesBL masterFeaturesBL;

    @Mock
    FeaturesDao jdbcTemplateDao;
    List<MasterFeaturesBean> masterFeaturesBeans;

    @Before
    public void setup() {
        masterFeaturesBeans= new ArrayList<>();
        MasterFeaturesBean masterFeaturesBean = new MasterFeaturesBean();
        masterFeaturesBean.setId(1);
        masterFeaturesBean.setName("UploadPage");
        masterFeaturesBean.setEnabled(true);
        masterFeaturesBeans.add(masterFeaturesBean);

    }

    @Test
    public void getClientValidation_Success() throws Exception {
        Assert.assertNull(masterFeaturesBL.clientValidation(null, "7640123a-fbde-4fe5-9812-581cd1e3a9c1"));
    }

    @Test
    public void serverValidation() throws Exception {
        UtilityBean<String> utilityBean = UtilityBean.<String>builder().pojoObject("7640123a-fbde-4fe5-9812-581cd1e3a9c1").build();
         Mockito.when(jdbcTemplateDao.getMasterFeatures()).thenReturn(masterFeaturesBeans);
        Assert.assertEquals(masterFeaturesBeans.get(0).getName(), masterFeaturesBL.serverValidation(utilityBean).get(0).getName());
    }

    @Test(expected = Exception.class)
    public void serverValidationCase2() throws Exception {
        UtilityBean<String> utilityBean = UtilityBean.<String>builder().pojoObject("7640123a-fbde-4fe5-9812-581cd1e3a9c1").build();
       masterFeaturesBeans.clear();
        Mockito.when(jdbcTemplateDao.getMasterFeatures()).thenReturn(masterFeaturesBeans);
        Assert.assertEquals(null, masterFeaturesBL.serverValidation(utilityBean));
    }

    @Test
    public void processData() throws Exception {
        Assert.assertEquals(masterFeaturesBeans.get(0).getName(), masterFeaturesBL.process(masterFeaturesBeans).getMasterFeaturesBeans().get(0).getName());
    }
}
