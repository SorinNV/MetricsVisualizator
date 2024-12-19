package com.example.metricsvisualizator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MetricsVisualizatorApplication {
	private static final Logger LOGGER =
			Logger.getLogger(String.valueOf(MetricsVisualizatorApplication.class));
	static final String path = "http://localhost:8081/";

	private static final Color BACKGROUND_COLOR = new Color(255,228,196);
	private static final int FRAME_WIDTH = 800;
	private static final int FRAME_HEIGHT = 600;

	public static void main(String[] args) {
		try {
			RestTemplateBuilder builder = new RestTemplateBuilder();
			RestTemplate restTemplate = builder.build();

			String stringData = "";

			stringData = restTemplate.getForObject(path + "input", String.class);
			System.out.println(stringData);
			List<InputData> inputData = new ArrayList<>();
			readInputDataFromString(stringData, inputData);

			JFrame f;
			JTable j;
			f = new JFrame();
			f.setTitle("JTable Example");

			String[][] data = new String[inputData.size()][4];
			int i = 0;
			for (InputData in : inputData) {
				data[i][0] = String.valueOf(in.getIdInputData());
				data[i][1] = String.valueOf(in.getITEquipmentPower());
				data[i][2] = String.valueOf(in.getTotalFacilityPower());
				data[i][3] = String.valueOf(in.getiTEquipmentUtilization());
				i++;
			}

			String[] columnNames = { "id", "ITEquipmentPower", "TotalFacilityPower", "ITEquipmentUtilization" };

			j = new JTable(data, columnNames);
			j.setBounds(30, 40, 200, 300);

			JScrollPane sp = new JScrollPane(j);
			f.add(sp);
			f.setSize(500, 200);

			f.setVisible(true);

			stringData = restTemplate.getForObject(path + "result", String.class);


			System.out.println(stringData);
			List<ResultData> resultData = new ArrayList<>();
			readResultDataFromString(stringData, resultData);

			double averagePUE = 0;
			double averageCPE = 0;

			averagePUE = resultData.stream().mapToDouble(ResultData::getPue).average().orElse(0);
			averageCPE = resultData.stream().mapToDouble(ResultData::getCpe).average().orElse(0);

			LOGGER.log(Level.INFO, "Average PUE: {0}", averagePUE);
			LOGGER.log(Level.INFO, "Average CPE: {0}", averageCPE);

			String defaultPath = "C:/Users/nikol/IdeaProjects/MAG_SEM3/MetricsVisualizator/src/main/resources/";
			drawAndSaveChart("PUE metric", "pue", createPueDataSet(resultData),
					defaultPath + "results/pueChart.png");
			drawAndSaveChart("CPE metric", "cpe (%)", createCpeDataSet(resultData),
					defaultPath + "results/cpeChart.png");
		} catch (Exception ex) {
			System.out.println(ex);
		}
	}

	public static void readResultDataFromString(String strData, List<ResultData> data) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			data.addAll(mapper.readerForListOf(ResultData.class).readValue(strData));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void readInputDataFromString(String strData, List<InputData> data) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			data.addAll(mapper.readerForListOf(InputData.class).readValue(strData));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void drawAndSaveChart(String title, String axTitle, XYDataset dataset, String savingName) {
		JFreeChart pueChart = ChartFactory.createXYLineChart(
				title,
				"time, ms", axTitle,
				dataset,
				PlotOrientation.VERTICAL,
				false, false, false);
		XYPlot plot = (XYPlot)pueChart.getPlot();
		plot.setBackgroundPaint(BACKGROUND_COLOR);
		JFrame pueFrame = new JFrame(title);
		pueFrame.setContentPane(new ChartPanel(pueChart));
		pueFrame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
		pueFrame.setLocationRelativeTo(null);
		pueFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		pueFrame.setVisible(true);

		BufferedImage img = new BufferedImage(pueFrame.getWidth(), pueFrame.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = img.createGraphics();
		pueFrame.printAll(g2d);
		g2d.dispose();
		try {
			ImageIO.write(img, "png", new File(savingName));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static XYDataset createDataSet(List<ResultData> data, String seriesName, ToDoubleFunction<ResultData> valueExtractor) {
		XYSeries series = new XYSeries(seriesName);

		data.sort(Comparator.comparingLong(dataElem -> dataElem.getTime().getTime()));
		ResultData startTimeData = data.get(0);
		for (ResultData resultData : data) {
			double value = valueExtractor.applyAsDouble(resultData);
			series.add((resultData.getTime().getTime() - startTimeData.getTime().getTime()), value);
		}

		return new XYSeriesCollection(series);
	}

	private static XYDataset createPueDataSet(List<ResultData> data) {
		return createDataSet(data, "PUE", ResultData::getPue);
	}

	private static XYDataset createCpeDataSet(List<ResultData> data) {
		return createDataSet(data, "CPE", ResultData::getCpe);
	}
}
