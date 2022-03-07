package io.github.vertxchina.vtalk.dialogpane;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.vertxchina.util.Common;
import io.github.vertxchina.vtalk.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class CenterPane extends ScrollPane {
  TextFlow chatHistory = new TextFlow();

  public CenterPane() {
    this.setContent(chatHistory);
    this.setPadding(new Insets(10));
    this.setStyle("-fx-background: #FFFFFF");
    this.vvalueProperty().bind(chatHistory.heightProperty());
  }

  public void appendChatHistory(JsonNode node) {
    var nickname = node.path("nickname");

    var time = node.path("time").asText();
    time = Common.HandlerDate(time);

    var wholeMessage = new VBox();
    wholeMessage.setPadding(new Insets(5));
    wholeMessage.setSpacing(3);

    var msgHead = new Text();
    var nn = nickname.isMissingNode() ? "我" : nickname.asText();
    var color = node.path("color").asText("#000");
    msgHead.setText(nn + " " + time);
    msgHead.setFill(Color.web(color));
    wholeMessage.getChildren().addAll(msgHead);

    var msg = node.path("message");
    placeNodesOnPane(msg, wholeMessage);

    if (nickname.isMissingNode())
      wholeMessage.setBackground(new Background(new BackgroundFill(Color.web("#b3e6b3"), new CornerRadii(5), Insets.EMPTY)));
    Platform.runLater(() -> chatHistory.getChildren().addAll(wholeMessage, new Text(System.lineSeparator()+System.lineSeparator())));
  }

  private Hyperlink generateHyperLink(String address){
    var hyperlink = new Hyperlink(address);
    hyperlink.setOnAction(e -> Application.hostServices.showDocument(address));
    return hyperlink;
  }

  private void placeNodesOnPane(JsonNode json, Pane pane){
    switch (json.getNodeType()){
      case STRING -> {
        var message = json.asText("");
        if(message.startsWith("http")){
          var msg = message.toLowerCase().trim();
          if(msg.endsWith("png")||msg.endsWith("jpg")||
              msg.endsWith("jpeg")||msg.endsWith("gif")){
            var imageview = new ImageView(message);
            if(imageview.getImage().isError())
              pane.getChildren().add(generateHyperLink(message));
            else {
              imageview.setPreserveRatio(true);
              if(this.getWidth() - 50 < imageview.getImage().getWidth())
                imageview.setFitWidth(this.getWidth() - 50);
              pane.getChildren().add(imageview);
            }
          }else
            pane.getChildren().add(generateHyperLink(message));
        }else{
          if((pane instanceof FlowPane flowPane) && message.contains("\n")){
            var msgs = message.split("\n");
            for(int i=0;i<msgs.length;i++){
              var msg = msgs[i];
              var text = new Text(msg);
              flowPane.getChildren().add(text);
              if(i<msgs.length-1 || message.endsWith("\n")){
                Region p = new Region();
                p.setPrefSize(this.getWidth()- text.getWrappingWidth() - 50, 0.0);
                flowPane.getChildren().add(p);
              }
            }
          }else{
            pane.getChildren().add(new Text(message));
          }
        }
      }
      case ARRAY -> {
        var flowPane = new FlowPane();
        flowPane.setRowValignment(VPos.BASELINE);
        for(int i=0;i<json.size();i++){
          var jsonNode = json.get(i).path("message");
          if(jsonNode.isMissingNode())
            placeNodesOnPane(json.get(i), flowPane);
          else
            placeNodesOnPane(json.get(i).path("message"), flowPane);
        }
        pane.getChildren().add(flowPane);
      }
      default -> pane.getChildren().add(new Text(json.asText()));
    }
  }
}
