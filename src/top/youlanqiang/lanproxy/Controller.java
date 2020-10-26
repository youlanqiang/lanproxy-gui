package top.youlanqiang.lanproxy;

import com.google.common.base.Strings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import top.youlanqiang.lanproxy.proxy.common.container.ContainerHelper;
import top.youlanqiang.lanproxy.proxy.ProxyClientContainer;


public class Controller {

    @FXML
    public TextField serverHost;

    @FXML
    public TextField serverPort;

    @FXML
    public TextField clientKey;

    @FXML
    public Button actionBtn;

    @FXML
    public TextArea log;

    @FXML
    public void actionApp(ActionEvent event){
       Button button =  (Button) event.getSource();
       if(button.getText().equals("启动")){
            if(Strings.isNullOrEmpty(serverHost.getText())){
                log.appendText("ServerHost不能为空\n");
                return;
           }
            if(Strings.isNullOrEmpty(serverPort.getText())){
                log.appendText("ServerPort不能为空\n");
                return;
            }
            if(!serverPort.getText().matches("[0-9]*")){
                log.appendText("ServerPort必须是数字\n");
                return;
            }
            if(Strings.isNullOrEmpty(clientKey.getText())){
                log.appendText("ClientKey不能为空\n");
                return;
            }
            new Thread(()->{
                log.appendText("启动连接\n");
                ProxyClientContainer.start(serverHost.getText(), Integer.valueOf(serverPort.getText()), clientKey.getText());
            }).start();

            button.setText("关闭");

       }else{
           try {
               ContainerHelper.stopContainers();
               log.appendText("成功关闭\n");
               button.setText("启动");
           }catch (Exception e){
               log.appendText(e.getMessage()+"\n");
           }
       }
    }


}
