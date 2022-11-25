package controller;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.AnchorPane;
 
 
public class LineChartController implements Initializable {
    @FXML
    private AnchorPane pane1;

	public static XYChart.Series series = new XYChart.Series();
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		// TODO Auto-generated method stub
		//defining the axes
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("距離(公里)");
        //creating the chart
        final LineChart<Number,Number> lineChart = 
                new LineChart<Number,Number>(xAxis,yAxis);
                
        lineChart.setTitle("剖面分析");
        //defining a series
//        series = new XYChart.Series();
        series.setName("高度(公尺)");

        
        Scene scene  = new Scene(lineChart,800,600);
        lineChart.getData().add(series);

        AnchorPane.setLeftAnchor(lineChart, 0.0);
        AnchorPane.setRightAnchor(lineChart, 0.0);
        AnchorPane.setTopAnchor(lineChart, 0.0);
        AnchorPane.setBottomAnchor(lineChart, 0.0);
        
        pane1.getChildren().add(lineChart);
	}
	
	void terminate() {

	}

}