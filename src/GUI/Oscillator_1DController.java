package GUI;

import Equation.Equation;
import Graphics.MatlabChart;
import Graphics.SomeChart;
import Graphics.AnimationOscillator_1D;
import NumericalMethods.Euler_KromerMethodOscillator;
import Models.Oscillator_1D;
import ResultsTable.Oscillator_1D_Table;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingNode;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;

import javax.swing.*;
import java.net.URL;
import java.util.*;

import static java.lang.Math.min;

public class Oscillator_1DController implements Initializable {
    // model parameters
    public TextField editStartX;
    public TextField editStartVelocity;
    public TextField editBodyMass;
    public TextField editRestitutionCoeff;
    public TextField editFrictionCoeff;

    // time parameters
    public TextField editStartTime;
    public TextField editFinishTime;
    public TextField editNumberOfCounts;

    // animation parameters
    public TextField editImpulse;
    public TextField editPeriodicAmplitude;
    public TextField editPeriodicPeriod;
    public TextField editAnimationRate;

    public Button btnToogleAnimation;
    public Button btnStopAnimation;
    public Button btnImpulse;
    public Button btnPeriodic;

    // put models into table
    public Button btnAddModel;
    public Button btnDeleteModel;
    public SplitMenuButton btnDrawModels;
    public javafx.scene.control.TableView tableModels;
    private ObservableList<Oscillator_1D> modelsData = FXCollections.observableArrayList();

    public AnchorPane paneMainMenu;
    public AnchorPane paneAnimation;

    // animation plots
    public TabPane tabPaneAnimationPlots;

    public Tab tabAnimationCoordinates;
    public Tab tabAnimationVelocity;
    public Tab tabAnimationEnergy;
    public Tab tabAnimationPhasePortret;

    public AnchorPane paneAnimationCoordinates;
    public AnchorPane paneAnimationVelocity;
    public AnchorPane paneAnimationEnergy;
    public AnchorPane paneAnimationPhasePortret;
    public AnchorPane paneAnimationOscillators;

    XChartPanel<XYChart> chartPanel_XT, chartPanel_VT, chartPanel_ET, chartPanel_VX;
    XChartPanel<XYChart> chartPanelOscillators;

    public AnimationOscillator_1D animationOscillator_1D;

    private enum PlotType {PT_COORDINATE, PT_VELOCITY, PT_ENERGY, PT_PHASE_PORTRET};
    private static HashMap<PlotType, String> plotTypePlotName;

    static {
        plotTypePlotName = new HashMap<PlotType, String>() {{
            put(PlotType.PT_COORDINATE, "Координаты X(t)");
            put(PlotType.PT_VELOCITY, "Скорость V(t)");
            put(PlotType.PT_ENERGY, "Полная энергия E(t)");
            put(PlotType.PT_PHASE_PORTRET, "Фазовый портрет V(X)");
        }};
    }

    private enum ExperimentDataType {EDT_SELECTED, EDT_FULL};

    private class ExperimentResult {
        public ArrayList<String> names;
        public ArrayList<ArrayList<Double>> px, pv, pt, pe;
        public Double minX, maxX;
        public Double minV, maxV;
        public Double minT, maxT;
        public Double minE, maxE;

        public ExperimentResult(ExperimentDataType edt) {
            names = new ArrayList();
            px = new ArrayList<>();
            pv = new ArrayList<>();
            pt = new ArrayList<>();
            pe = new ArrayList<>();

            ArrayList<Oscillator_1D> data = new ArrayList<>();
            if (edt == ExperimentDataType.EDT_SELECTED)
                data = GetSelectedModels();
            else {
                for (int i = 0; i < modelsData.size(); ++i) data.add(modelsData.get(i));
            }

            minX = minV = minT = minE = 1e10;
            maxX = maxV = maxT = maxE = -1e10;

            for (int i = 0; i < data.size(); ++i) {
                Oscillator_1D cur = data.get(i).clone();
                ArrayList<Double> t_arr = new ArrayList<>();
                ArrayList<Double> x_arr = new ArrayList<>();
                ArrayList<Double> v_arr = new ArrayList<>();
                ArrayList<Double> e_arr = new ArrayList<>();

                for (int j = 0; j <= data.get(i).getN(); ++j) {
                    t_arr.add(cur.getT0());
                    x_arr.add(cur.getX0());
                    v_arr.add(cur.getV0());
                    e_arr.add(cur.getEnergy());
                    Euler_KromerMethodOscillator.NextValues(cur);
                }

                names.add(String.format("Model № %d", cur.getNumber()));
                px.add(x_arr);
                pv.add(v_arr);
                pt.add(t_arr);
                pe.add(e_arr);

                minX = Math.min(minX, Collections.min(px.get(i)));
                maxX = Math.max(maxX, Collections.max(px.get(i)));

                minV = Math.min(minV, Collections.min(pv.get(i)));
                maxV = Math.max(maxV, Collections.max(pv.get(i)));

                minT = Math.min(minT, Collections.min(pt.get(i)));
                maxT = Math.max(maxT, Collections.max(pt.get(i)));

                minE = Math.min(minE, Collections.min(pe.get(i)));
                maxE = Math.max(maxE, Collections.max(pe.get(i)));
            }
        }
    }

    public void addItemToMenu(ObservableList<MenuItem> items, PlotType pt) {
        items.add(new MenuItem(plotTypePlotName.get(pt)));
        items.get(items.size() - 1).setOnAction(f -> {
            drawPlot(pt);
        });
    }

    public XChartPanel<XYChart> createAndAddChartToTab(AnchorPane pane) {
        SomeChart<XYChart> chartMatlab = new MatlabChart();
        XYChart chart = chartMatlab.getSizedChart(new ArrayList<ArrayList<Double>>(), new ArrayList<ArrayList<Double>>(), new ArrayList<String>(),
                (int) Math.max(pane.getWidth(), pane.getMinWidth()),
                (int) Math.max(pane.getHeight(), pane.getMinHeight()));

        XChartPanel<XYChart> pnl = new XChartPanel(chart);
        SwingNode swingNode = new SwingNode();
        swingNode.setContent(pnl);
        pane.getChildren().add(swingNode);
        return pnl;
    }

    public ArrayList<Oscillator_1D> GetSelectedModels() {
        ObservableList<Oscillator_1D> obs = tableModels.getSelectionModel().getSelectedItems();
        ArrayList<Oscillator_1D> res = new ArrayList<>();
        for (int i = 0; i < obs.size(); ++i) res.add(obs.get(i));
        return res;
    }

    public void AddResizeListenerToPane(AnchorPane pane, XChartPanel<XYChart> chartPanel) {
        pane.widthProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) {
//                chartPanel.setPreferredSize(new Dimension((int)pane.getWidth(), (int)pane.getHeight()));
                //chartPanel.setPreferredSize(new Dimension(700, 700));

//                chartPanel.setSize(new Dimension(700, 700));
//                System.console().printf("%e", chartPanel.getPreferredSize().getWidth());
//                chartPanel.repaint();

                //chartPanel.getChart().set;
                //XYChart tmp; tmp.getWidth()
            }
        });
        pane.heightProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneHeight, Number newSceneHeight) {
//                System.out.println("Height: " + newSceneHeight);
            }
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) throws NumberFormatException {
        addItemToMenu(btnDrawModels.getItems(), PlotType.PT_COORDINATE);
        addItemToMenu(btnDrawModels.getItems(), PlotType.PT_VELOCITY);
        addItemToMenu(btnDrawModels.getItems(), PlotType.PT_ENERGY);
        addItemToMenu(btnDrawModels.getItems(), PlotType.PT_PHASE_PORTRET);

        btnDrawModels.getItems().add(new MenuItem("Сводная таблица"));
        btnDrawModels.getItems().get(btnDrawModels.getItems().size() - 1).setOnAction(f -> {
            createSummaryTable();
        });

        tableModels.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        chartPanel_XT = createAndAddChartToTab(paneAnimationCoordinates);
        chartPanel_VT = createAndAddChartToTab(paneAnimationVelocity);
        chartPanel_ET = createAndAddChartToTab(paneAnimationEnergy);
        chartPanel_VX = createAndAddChartToTab(paneAnimationPhasePortret);
        chartPanelOscillators = createAndAddChartToTab(paneAnimationOscillators);

        chartPanelOscillators.getChart().getStyler().setLegendVisible(false);
        chartPanelOscillators.getChart().getStyler().setMarkerSize(8);

        //AddResizeListenerToPane(paneAnimationCoordinates, chartPanel_XT);

        animationOscillator_1D = new AnimationOscillator_1D(btnToogleAnimation, btnStopAnimation,
                () -> this.GetSelectedModels(), chartPanel_XT, chartPanel_VT, chartPanel_ET, chartPanel_VX,
                chartPanelOscillators, () -> UpdateCharts());

        UpdateCharts();
    }

    public void drawPlot(PlotType pt) {

        if (modelsData.isEmpty()) return;

        setStartTimes();

        ArrayList<ArrayList<Double>> px = new ArrayList<>();
        ArrayList<ArrayList<Double>> py = new ArrayList<>();
        ExperimentResult result = new ExperimentResult(ExperimentDataType.EDT_FULL);

        if (pt == PlotType.PT_COORDINATE) {
            px = result.pt;
            py = result.px;
        } else if (pt == PlotType.PT_VELOCITY) {
            px = result.pt;
            py = result.pv;
        } else if (pt == PlotType.PT_ENERGY) {
            px = result.pt;
            py = result.pe;
        } else if (pt == PlotType.PT_PHASE_PORTRET) {
            px = result.px;
            py = result.pv;
        }

        SomeChart<XYChart> chartMatlab = new MatlabChart();
        XYChart chartSolutions = chartMatlab.getChart(px, py, result.names);
        chartSolutions.setTitle(plotTypePlotName.get(pt));
        new SwingWrapper(chartSolutions).displayChart().setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    }

    private void setParameters(Oscillator_1D oscillator1D) {
        oscillator1D.setGamma(Double.parseDouble(editFrictionCoeff.getText()));
        oscillator1D.setM(Double.parseDouble(editBodyMass.getText()));
        oscillator1D.setN(Integer.parseInt(editNumberOfCounts.getText()));
        oscillator1D.setT0(Double.parseDouble(editStartTime.getText()));
        oscillator1D.setT1(Double.parseDouble(editFinishTime.getText()));
        oscillator1D.setX0(Double.parseDouble(editStartX.getText()));
        oscillator1D.setV0(Double.parseDouble(editStartVelocity.getText()));
        oscillator1D.setK(Double.parseDouble(editRestitutionCoeff.getText()));
    }

    public void addModel() {
        // Read fields and add it to the table
        Oscillator_1D cur = new Oscillator_1D();
        setParameters(cur);

        ObservableList<TableColumn> columns = tableModels.getColumns();

        columns.get(0).setCellValueFactory(
                new PropertyValueFactory<Oscillator_1D, String>("number"));
        columns.get(1).setCellValueFactory(
                new PropertyValueFactory<Equation, Double>("x0"));
        columns.get(2).setCellValueFactory(
                new PropertyValueFactory<Equation, Double>("v0"));
        columns.get(3).setCellValueFactory(
                new PropertyValueFactory<Equation, String>("m"));
        columns.get(4).setCellValueFactory(
                new PropertyValueFactory<Equation, String>("k"));
        columns.get(5).setCellValueFactory(
                new PropertyValueFactory<Equation, String>("gamma"));
        modelsData.add(cur);
        tableModels.setItems(modelsData);
        tableModels.getSelectionModel().select(tableModels.getItems().size() - 1);
    }

    public void deleteFromTable() {
        Integer idx = tableModels.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            modelsData.remove(idx, idx + 1);
            tableModels.setItems(modelsData);
        }
    }

    private void SetChartMinMaxAxis(XYChart chart, Double minX, Double maxX, Double minY, Double maxY) {
        chart.getStyler().setXAxisMin(minX);
        chart.getStyler().setXAxisMax(maxX);
        chart.getStyler().setYAxisMin(minY);
        chart.getStyler().setYAxisMax(maxY);
    }

    public void StartAnimation() {
        if (GetSelectedModels().isEmpty()) return;

        if (animationOscillator_1D.ProcessingExperiment()) {
            animationOscillator_1D.Start(0.0, 0);
            return;
        }

        setStartTimes();
        ExperimentResult res = new ExperimentResult(ExperimentDataType.EDT_SELECTED);

        Double minT = res.minT;
        Double maxT = res.maxT;

        // TODO : read this speed from the edit field
        Integer viewSpeed = Integer.parseInt(editAnimationRate.getText());
        Double wholeTime = maxT - minT;
        Integer wholePoints = res.pt.get(0).size();
        Double screens = (double)wholePoints / AnimationOscillator_1D.maxPointsOnChart;
        Double timeAxisWidth = wholeTime / screens;
        Integer pointsPerTick = viewSpeed;

        maxT = minT + timeAxisWidth;

        SetChartMinMaxAxis(chartPanel_XT.getChart(), minT, maxT, res.minX, res.maxX);
        SetChartMinMaxAxis(chartPanel_VT.getChart(), minT, maxT, res.minV, res.maxV);
        SetChartMinMaxAxis(chartPanel_ET.getChart(), minT, maxT, res.minE, res.maxE);
        SetChartMinMaxAxis(chartPanel_VX.getChart(), res.minX, res.maxX, res.minV, res.maxV);
        SetChartMinMaxAxis(chartPanelOscillators.getChart(), res.minX, res.maxX, -0.5, res.pt.size() - 0.5);

        animationOscillator_1D.Start(timeAxisWidth, pointsPerTick);
    }

    public void StopAnimation() {
        animationOscillator_1D.Stop();
    }

    public void OnBtnImpulseClick() {
        animationOscillator_1D.AddImpulseForce(Double.parseDouble(editImpulse.getText()));
    }

    public void OnBtnPeriodicClick() {
        animationOscillator_1D.AddPeriodicForce(Double.parseDouble(editPeriodicAmplitude.getText()),
                Double.parseDouble(editPeriodicPeriod.getText()));
    }

    public void UpdateCharts() {
        if (chartPanelOscillators == null) return;
        chartPanelOscillators.repaint();
        if (tabAnimationCoordinates.isSelected())
            chartPanel_XT.repaint();
        else if (tabAnimationVelocity.isSelected())
            chartPanel_VT.repaint();
        else if (tabAnimationEnergy.isSelected())
            chartPanel_ET.repaint();
        else
            chartPanel_VX.repaint();
    }


    // TODO : REWRITE THIS
    public void createSummaryTable() {
        if (modelsData.isEmpty()) return;
        setStartTimes();
        ExperimentResult experimentResult = new ExperimentResult(ExperimentDataType.EDT_FULL);

        ArrayList<ArrayList<Double>> data = new ArrayList<>();
        ArrayList<ArrayList<Double>> modelInfo = new ArrayList<>();

        for (int i = 0; i < modelsData.size(); ++i) {
            Oscillator_1D cur = modelsData.get(i).clone();
            data.add(new ArrayList<>());

            modelInfo.add(new ArrayList<>());
            modelInfo.get(i).add(modelsData.get(i).getX0());
            modelInfo.get(i).add(modelsData.get(i).getV0());
            modelInfo.get(i).add(modelsData.get(i).getM());
            modelInfo.get(i).add(modelsData.get(i).getK());
            modelInfo.get(i).add(modelsData.get(i).getGamma());

            for (int j = 0; j <= modelsData.get(i).getN(); ++j) {
                data.get(i).add(cur.getT0());
                data.get(i).add(cur.getX0());
                data.get(i).add(cur.getV0());
                data.get(i).add(cur.getEnergy());
                Euler_KromerMethodOscillator.NextValues(cur);
            }
        }
        ArrayList<JTable> tables = Oscillator_1D_Table.GetTable(data, modelInfo);
        for (JTable jtable: tables) {
            JFrame frame = new JFrame("Table");
            frame.add(new JScrollPane(jtable));
            frame.setSize(jtable.getColumnModel().getTotalColumnWidth() + 20, 500);
            frame.setVisible(true);
        }
    }

    public void setStartTimes() throws NumberFormatException {
        for (Oscillator_1D oscillator : modelsData) {
            oscillator.setT0(Double.parseDouble(editStartTime.getText()));
            oscillator.setT1(Double.parseDouble(editFinishTime.getText()));
            oscillator.setN(Integer.parseInt(editNumberOfCounts.getText()));
        }
    }
}
