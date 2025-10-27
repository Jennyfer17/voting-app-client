package com.example;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import br.com.grupo05.model.Candidato;
import br.com.grupo05.model.Eleitor;
import br.com.grupo05.util.Votacao;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * JavaFX client for the RMI voting server.
 * Connects to registry on localhost:8080 with name "localhost/Votacao".
 */
public class Main extends Application {

    // ==================== CONFIGURAÇÕES DE HORÁRIO ====================
    private static final LocalTime REGISTRO_INICIO = LocalTime.of(8, 0);   
    private static final LocalTime REGISTRO_FIM = LocalTime.of(12, 0);    
    private static final LocalTime VOTACAO_INICIO = LocalTime.of(8, 0);  
    private static final LocalTime VOTACAO_FIM = LocalTime.of(8, 45);     

    private Votacao votacao;
    private ObservableList<Candidato> candidatos = FXCollections.observableArrayList();

    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduleChecker;

    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("🗳️ Sistema de Votação");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // Top: title and timer
        Label title = new Label("🗳️ Sistema de Votação Eletrónica");
        title.getStyleClass().add("title");
        Label timerLabel = new Label("⏰ Inicializando...");
        timerLabel.getStyleClass().add("timer");
        HBox topBar = new HBox(30, title, timerLabel);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 0, 20, 0));

        // Initial menu: quatro botões principais
        Button regCandidatoBtn = new Button("📝 Registar Candidato");
        regCandidatoBtn.getStyleClass().add("big-button");
        Button realizarVotacaoBtn = new Button("✅ Realizar Votação");
        realizarVotacaoBtn.getStyleClass().add("big-button");
        Button atualizarVotoBtn = new Button("🔄 Atualizar Voto");
        atualizarVotoBtn.getStyleClass().add("big-button");
        Button verResultadosBtn = new Button("📊 Ver Resultados");
        verResultadosBtn.getStyleClass().add("big-button");
        
        VBox initialMenu = new VBox(20, regCandidatoBtn, realizarVotacaoBtn, atualizarVotoBtn, verResultadosBtn);
        initialMenu.setPadding(new Insets(40));
        initialMenu.setAlignment(Pos.CENTER);
        initialMenu.setPrefWidth(400);

        // Candidate registration pane
        VBox candidateRegisterPane = new VBox(15);
        candidateRegisterPane.setAlignment(Pos.TOP_CENTER);
        Label candTitle = new Label("📝 Registo de Candidato");
        candTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
        TextField candNameField = new TextField();
        candNameField.setPromptText("👤 Nome do candidato");
        candNameField.setPrefWidth(350);
        
        TextField candNumberField = new TextField();
        candNumberField.setPromptText("🔢 Número do candidato (ex: 12)");
        candNumberField.setPrefWidth(350);
        
        ChoiceBox<Candidato.Position> candPositionChoice = new ChoiceBox<>();
        candPositionChoice.getItems().addAll(Candidato.Position.CHEFE, Candidato.Position.CHEFE_ADJUNTO);
        candPositionChoice.setValue(Candidato.Position.CHEFE);
        candPositionChoice.setPrefWidth(350);
        
        Label posLabel = new Label("👔 Cargo:");
        HBox candPosRow = new HBox(10, posLabel, candPositionChoice);
        candPosRow.setAlignment(Pos.CENTER_LEFT);
        
        Button regCandidateSubmit = new Button("✅ Confirmar Registo");
        Button candBackBtn = new Button("⬅️ Voltar");
        candBackBtn.setOnAction(ev -> root.setCenter(initialMenu));
        
        HBox candButtons = new HBox(10, regCandidateSubmit, candBackBtn);
        candButtons.setAlignment(Pos.CENTER);
        
        candidateRegisterPane.getChildren().addAll(candTitle, candNameField, candNumberField, candPosRow, candButtons);

        // Update vote pane (atualização de voto)
        VBox updateVotePane = new VBox(15);
        updateVotePane.setAlignment(Pos.TOP_CENTER);
        Label updateVoteTitle = new Label("🔄 Atualizar Voto");
        updateVoteTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
        TextField updateVoterName = new TextField();
        updateVoterName.setPromptText("👤 Seu identificador");
        updateVoterName.setPrefWidth(350);
        updateVoterName.setEditable(false);
        
        Label updateSelectLabel = new Label("📋 Selecione o novo candidato:");
        ListView<Candidato> updateListView = new ListView<>(candidatos);
        updateListView.setPrefWidth(350);
        updateListView.setPrefHeight(250);
        
        Button confirmUpdateVoteBtn = new Button("✅ Confirmar Atualização");
        Button cancelUpdateVoteBtn = new Button("❌ Cancelar");
        Button updateVoteBackBtn = new Button("⬅️ Voltar");
        updateVoteBackBtn.setOnAction(ev -> root.setCenter(initialMenu));
        
        HBox updateVoteActions = new HBox(10, confirmUpdateVoteBtn, cancelUpdateVoteBtn, updateVoteBackBtn);
        updateVoteActions.setAlignment(Pos.CENTER);
        
        updateVotePane.getChildren().addAll(updateVoteTitle, updateVoterName, updateSelectLabel, updateListView, updateVoteActions);

        // Voting pane
        VBox votePane = new VBox(15);
        votePane.setAlignment(Pos.TOP_CENTER);
        Label voteTitle = new Label("✅ Realizar Votação");
        voteTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
        TextField voterName = new TextField();
        voterName.setPromptText("👤 Seu identificador");
        voterName.setPrefWidth(350);
        voterName.setEditable(false);
        
        Label selectLabel = new Label("📋 Selecione o candidato:");
        ListView<Candidato> listView = new ListView<>(candidatos);
        listView.setPrefWidth(350);
        listView.setPrefHeight(250);
        
        Button confirmVoteBtn = new Button("✅ Confirmar Voto");
        Button cancelVoteBtn = new Button("❌ Cancelar");
        Button voteBackBtn = new Button("⬅️ Voltar");
        voteBackBtn.setOnAction(ev -> root.setCenter(initialMenu));
        
        HBox voteActions = new HBox(10, confirmVoteBtn, cancelVoteBtn, voteBackBtn);
        voteActions.setAlignment(Pos.CENTER);
        
        votePane.getChildren().addAll(voteTitle, voterName, selectLabel, listView, voteActions);

        // Results pane (tela completa de resultados com ScrollPane)
        VBox resultsContentPane = new VBox(20);
        resultsContentPane.setAlignment(Pos.TOP_CENTER);
        resultsContentPane.setPadding(new Insets(30));
        
        Label resultsMainTitle = new Label("📊 RESULTADOS DA VOTAÇÃO");
        resultsMainTitle.setFont(Font.font("System", FontWeight.BOLD, 28));
        resultsMainTitle.setStyle("-fx-text-fill: #667eea;");
        
        VBox resultsContainer = new VBox(30);
        resultsContainer.setAlignment(Pos.TOP_CENTER);
        resultsContainer.setPrefWidth(700);
        
        ScrollPane resultsScrollPane = new ScrollPane(resultsContentPane);
        resultsScrollPane.setFitToWidth(true);
        resultsScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        VBox resultsPane = new VBox(15);
        resultsPane.setAlignment(Pos.TOP_CENTER);
        
        Button refreshResultsBtn = new Button("🔄 Atualizar Resultados");
        Button resultsBackBtn = new Button("⬅️ Voltar ao Menu");
        HBox resultsButtons = new HBox(15, refreshResultsBtn, resultsBackBtn);
        resultsButtons.setAlignment(Pos.CENTER);
        resultsButtons.setPadding(new Insets(20, 0, 20, 0));
        
        resultsPane.getChildren().addAll(resultsScrollPane, resultsButtons);
        resultsBackBtn.setOnAction(ev -> root.setCenter(initialMenu));
        
        // Função para carregar e formatar resultados
        Runnable loadResults = () -> {
            new Thread(() -> {
                try {
                    Map<Integer, Integer> votos = votacao.getVotosPorCandidato();
                    List<Candidato> candList = votacao.getCandidatos();
                    
                    Platform.runLater(() -> {
                        resultsContainer.getChildren().clear();
                        
                        // Separa candidatos por cargo
                        Map<Candidato.Position, List<Candidato>> porCargo = candList.stream()
                            .collect(Collectors.groupingBy(c -> c.getPosition() != null ? c.getPosition() : Candidato.Position.CHEFE));
                        
                        // Para cada cargo, mostra os resultados
                        for (Candidato.Position cargo : new Candidato.Position[]{Candidato.Position.CHEFE, Candidato.Position.CHEFE_ADJUNTO}) {
                            List<Candidato> candidatosCargo = porCargo.get(cargo);
                            if (candidatosCargo == null || candidatosCargo.isEmpty()) continue;
                            
                            // Container do cargo
                            VBox cargoBox = new VBox(15);
                            cargoBox.setStyle("-fx-background-color: white; -fx-background-radius: 16px; " +
                                            "-fx-padding: 25; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 4);");
                            cargoBox.setPrefWidth(650);
                            
                            // Título do cargo
                            String cargoEmoji = cargo == Candidato.Position.CHEFE ? "👑" : "⭐";
                            String cargoNome = cargo == Candidato.Position.CHEFE ? "CHEFE" : "CHEFE ADJUNTO";
                            Label cargoTitle = new Label(cargoEmoji + " RESULTADOS - " + cargoNome);
                            cargoTitle.setFont(Font.font("System", FontWeight.BOLD, 22));
                            cargoTitle.setStyle("-fx-text-fill: #2d3748;");
                            
                            // Separador
                            Label separator = new Label("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                            separator.setStyle("-fx-text-fill: #cbd5e0;");
                            
                            cargoBox.getChildren().addAll(cargoTitle, separator);
                            
                            // Ordena candidatos deste cargo por votos
                            List<Candidato> candidatosOrdenados = candidatosCargo.stream()
                                .sorted((c1, c2) -> {
                                    int v1 = votos.getOrDefault(c1.getNumero(), 0);
                                    int v2 = votos.getOrDefault(c2.getNumero(), 0);
                                    return Integer.compare(v2, v1);
                                })
                                .toList();
                            
                            // Calcula total de votos deste cargo
                            int totalVotosCargo = candidatosOrdenados.stream()
                                .mapToInt(c -> votos.getOrDefault(c.getNumero(), 0))
                                .sum();
                            
                            // Exibe cada candidato
                            for (int i = 0; i < candidatosOrdenados.size(); i++) {
                                Candidato cand = candidatosOrdenados.get(i);
                                int numVotos = votos.getOrDefault(cand.getNumero(), 0);
                                double percentual = totalVotosCargo > 0 ? (numVotos * 100.0 / totalVotosCargo) : 0;
                                
                                VBox candidatoBox = new VBox(8);
                                candidatoBox.setPadding(new Insets(15));
                                
                                // Estilo especial para o vencedor
                                if (i == 0 && numVotos > 0) {
                                    candidatoBox.setStyle("-fx-background-color: linear-gradient(to right, #ffd700 0%, #ffed4e 100%); " +
                                                         "-fx-background-radius: 12px; -fx-border-color: #f59e0b; " +
                                                         "-fx-border-width: 2px; -fx-border-radius: 12px;");
                                    
                                    Label vencedorLabel = new Label("🎉 VENCEDOR");
                                    vencedorLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                                    vencedorLabel.setStyle("-fx-text-fill: #92400e;");
                                    candidatoBox.getChildren().add(vencedorLabel);
                                } else {
                                    candidatoBox.setStyle("-fx-background-color: #f7fafc; -fx-background-radius: 12px; " +
                                                         "-fx-border-color: #e2e8f0; -fx-border-width: 1px; -fx-border-radius: 12px;");
                                }
                                
                                // Nome e número
                                Label nomeLabel = new Label("👤 " + cand.getNome() + " (#" + cand.getNumero() + ")");
                                nomeLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
                                nomeLabel.setStyle("-fx-text-fill: #1a202c;");
                                
                                // Votos e percentual
                                Label votosLabel = new Label(String.format("📊 %d voto%s (%.1f%%)", 
                                    numVotos, numVotos != 1 ? "s" : "", percentual));
                                votosLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));
                                votosLabel.setStyle("-fx-text-fill: #4a5568;");
                                
                                // Barra de progresso
                                HBox progressBar = new HBox();
                                progressBar.setPrefHeight(25);
                                progressBar.setMaxWidth(600);
                                progressBar.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 8px;");
                                
                                HBox progressFill = new HBox();
                                progressFill.setPrefHeight(25);
                                progressFill.setPrefWidth(Math.max(5, (percentual / 100.0) * 600));
                                
                                if (i == 0 && numVotos > 0) {
                                    progressFill.setStyle("-fx-background-color: linear-gradient(to right, #f59e0b 0%, #d97706 100%); " +
                                                         "-fx-background-radius: 8px;");
                                } else {
                                    progressFill.setStyle("-fx-background-color: linear-gradient(to right, #667eea 0%, #764ba2 100%); " +
                                                         "-fx-background-radius: 8px;");
                                }
                                
                                progressBar.getChildren().add(progressFill);
                                
                                candidatoBox.getChildren().addAll(nomeLabel, votosLabel, progressBar);
                                cargoBox.getChildren().add(candidatoBox);
                            }
                            
                            // Total de votos do cargo
                            Label totalLabel = new Label("📈 Total de votos nesta categoria: " + totalVotosCargo);
                            totalLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                            totalLabel.setStyle("-fx-text-fill: #718096; -fx-padding: 10 0 0 0;");
                            cargoBox.getChildren().add(totalLabel);
                            
                            resultsContainer.getChildren().add(cargoBox);
                        }
                        
                        // Data/hora de geração
                        Label timestampLabel = new Label("🕐 Atualizado em: " + 
                            java.time.LocalDateTime.now().format(
                                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
                        timestampLabel.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 12px;");
                        resultsContainer.getChildren().add(timestampLabel);
                        
                        resultsContentPane.getChildren().clear();
                        resultsContentPane.getChildren().addAll(resultsMainTitle, resultsContainer);
                    });
                    
                } catch (RemoteException e) {
                    Platform.runLater(() -> {
                        showError("❌ Erro ao obter resultados: " + e.getMessage());
                    });
                }
            }).start();
        };
        
        refreshResultsBtn.setOnAction(ev -> loadResults.run());

        // Combine - start with initial menu in center
        root.setTop(topBar);
        root.setCenter(initialMenu);

        Scene scene = new Scene(root, 900, 650);
        // load css
        try {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("CSS não encontrado: " + e.getMessage());
        }

        primaryStage.setScene(scene);
        primaryStage.show();

        // schedule checker: checks configured windows and enables/disables buttons
        scheduleChecker = scheduler.scheduleAtFixedRate(() -> {
            LocalTime now = LocalTime.now();
            
            // Verifica se está no horário de registo
            boolean regAllowed = !now.isBefore(REGISTRO_INICIO) && !now.isAfter(REGISTRO_FIM);
            
            // Verifica se está no horário de votação
            boolean voteAllowed = !now.isBefore(VOTACAO_INICIO) && !now.isAfter(VOTACAO_FIM);
            
            // Resultados só ficam disponíveis após o término da votação
            boolean resultsAllowed = now.isAfter(VOTACAO_FIM);
            
            Platform.runLater(() -> {
                regCandidatoBtn.setDisable(!regAllowed);
                realizarVotacaoBtn.setDisable(!voteAllowed);
                atualizarVotoBtn.setDisable(!voteAllowed); // Atualização só durante votação
                verResultadosBtn.setDisable(!resultsAllowed);
                
                // update timerLabel with current status
                String status;
                if (regAllowed) {
                    status = "📝 Registo ativo até " + REGISTRO_FIM.format(timeFormatter);
                } else if (voteAllowed) {
                    status = "✅ Votação ativa até " + VOTACAO_FIM.format(timeFormatter);
                } else if (now.isBefore(REGISTRO_INICIO)) {
                    status = "⏳ Registo inicia às " + REGISTRO_INICIO.format(timeFormatter);
                } else if (now.isAfter(REGISTRO_FIM) && now.isBefore(VOTACAO_INICIO)) {
                    status = "⏳ Votação inicia às " + VOTACAO_INICIO.format(timeFormatter);
                } else if (resultsAllowed) {
                    status = "🎉 Votação encerrada - Resultados disponíveis";
                } else {
                    status = "⏸️ Sistema em standby";
                }
                timerLabel.setText(status);
            });
        }, 0, 1, TimeUnit.SECONDS);

        // Connect to RMI in background and load candidates
        new Thread(() -> {
            try {
                connectToServer();
                List<Candidato> list = votacao.getCandidatos();
                Platform.runLater(() -> {
                    candidatos.setAll(list);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("❌ Erro ao conectar ao servidor RMI: " + e.getMessage());
                });
            }
        }).start();

        // initial menu actions
        regCandidatoBtn.setOnAction(ev -> root.setCenter(candidateRegisterPane));
        realizarVotacaoBtn.setOnAction(ev -> root.setCenter(createVoteLoginPane(root, votePane, initialMenu, false)));
        atualizarVotoBtn.setOnAction(ev -> root.setCenter(createVoteLoginPane(root, updateVotePane, initialMenu, true)));
        verResultadosBtn.setOnAction(ev -> {
            root.setCenter(resultsPane);
            loadResults.run();
        });

        // initial availability: disabled until appropriate time
        regCandidatoBtn.setDisable(true);
        realizarVotacaoBtn.setDisable(true);
        atualizarVotoBtn.setDisable(true);
        verResultadosBtn.setDisable(true);

        // Candidate registration submit
        regCandidateSubmit.setOnAction(ev -> {
            String nome = candNameField.getText().trim();
            String numText = candNumberField.getText().trim();
            Candidato.Position pos = candPositionChoice.getValue();
            
            if (nome.isEmpty() || numText.isEmpty()) {
                showAlert("⚠️ Atenção", "Por favor, informe o nome e número do candidato.");
                return;
            }
            
            int numero;
            try {
                numero = Integer.parseInt(numText);
            } catch (NumberFormatException ex) {
                showAlert("⚠️ Erro", "Número do candidato inválido. Use apenas números.");
                return;
            }
            
            if (pos == null) pos = Candidato.Position.CHEFE;
            Candidato novo = new Candidato(nome, numero, pos);
            
            new Thread(() -> {
                try {
                    boolean ok = votacao.registrarCandidato(novo);
                    if (ok) {
                        List<Candidato> updated = votacao.getCandidatos();
                        Platform.runLater(() -> {
                            candidatos.setAll(updated);
                            showAlert("✅ Sucesso", "Candidato " + nome + " registado com sucesso!");
                            candNameField.clear();
                            candNumberField.clear();
                        });
                    } else {
                        Platform.runLater(() -> showAlert("❌ Erro", "Falha ao registar candidato. Número pode já existir."));
                    }
                } catch (Exception ex) {
                    Platform.runLater(() -> showError("❌ Erro ao registar candidato: " + ex.getMessage()));
                }
            }).start();
        });

        // Update vote confirm/cancel
        cancelUpdateVoteBtn.setOnAction(ev -> {
            updateListView.getSelectionModel().clearSelection();
        });

        confirmUpdateVoteBtn.setOnAction(ev -> {
            Candidato selecionado = updateListView.getSelectionModel().getSelectedItem();
            String id = updateVoterName.getText().trim();
            
            if (id.isEmpty()) {
                showAlert("⚠️ Atenção", "Por favor, identifique-se primeiro.");
                return;
            }
            
            if (selecionado == null) {
                showAlert("⚠️ Atenção", "Por favor, selecione um candidato para atualizar seu voto.");
                return;
            }

            boolean confirmed = confirmDialog("🔄 Confirmar Atualização de Voto", 
                "Deseja realmente ATUALIZAR seu voto para:\n\n👤 " + selecionado.getNome() + 
                " (Número: " + selecionado.getNumero() + ")\n\nSeu voto anterior será substituído!");
            
            if (!confirmed) return;

            new Thread(() -> {
                try {
                    Eleitor eleitor = new Eleitor(id, null);
                    
                    // Verifica se está votando em si mesmo (se for candidato)
                    boolean votandoEmSimesmo = votacao.isVotandoEmSiMesmo(eleitor, selecionado);
                    if (votandoEmSimesmo) {
                        Platform.runLater(() -> showError("❌ Você não pode votar em si mesmo!"));
                        return;
                    }
                    
                    // Verifica se já votou antes (deve ter votado para poder atualizar)
                    boolean jaVotou = votacao.isVotandoSegundaVez(eleitor);
                    if (!jaVotou) {
                        Platform.runLater(() -> showError("❌ Você ainda não votou. Use a opção 'Realizar Votação' primeiro."));
                        return;
                    }

                    // Atualiza o voto
                    boolean ok = votacao.atualizarVoto(eleitor, selecionado);
                    if (ok) {
                        Platform.runLater(() -> {
                            showAlert("✅ Sucesso", "🎉 Voto atualizado com sucesso!\n\nSeu novo voto foi registrado.");
                            updateVoterName.clear();
                            updateListView.getSelectionModel().clearSelection();
                            root.setCenter(initialMenu);
                        });
                    } else {
                        Platform.runLater(() -> showError("❌ Falha ao atualizar voto. Tente novamente."));
                    }
                } catch (RemoteException e) {
                    Platform.runLater(() -> showError("❌ Erro de comunicação: " + e.getMessage()));
                } catch (Exception e) {
                    Platform.runLater(() -> showError("❌ Erro: " + e.getMessage()));
                }
            }).start();
        });

        // Voting confirm/cancel
        cancelVoteBtn.setOnAction(ev -> {
            listView.getSelectionModel().clearSelection();
        });

        confirmVoteBtn.setOnAction(ev -> {
            Candidato selecionado = listView.getSelectionModel().getSelectedItem();
            String id = voterName.getText().trim();
            
            if (id.isEmpty()) {
                showAlert("⚠️ Atenção", "Por favor, identifique-se primeiro.");
                return;
            }
            
            if (selecionado == null) {
                showAlert("⚠️ Atenção", "Por favor, selecione um candidato para votar.");
                return;
            }

            boolean confirmed = confirmDialog("🗳️ Confirmar Voto", 
                "Deseja realmente votar em:\n\n👤 " + selecionado.getNome() + 
                " (Número: " + selecionado.getNumero() + ")\n\nEsta ação não pode ser desfeita!");
            
            if (!confirmed) return;

            new Thread(() -> {
                try {
                    Eleitor eleitor = new Eleitor(id, null);
                    
                    // Verifica se está votando em si mesmo (se for candidato)
                    boolean votandoEmSimesmo = votacao.isVotandoEmSiMesmo(eleitor, selecionado);
                    if (votandoEmSimesmo) {
                        Platform.runLater(() -> showError("❌ Você não pode votar em si mesmo!"));
                        return;
                    }
                    
                    // Verifica se já votou antes
                    boolean jaVotou = votacao.isVotandoSegundaVez(eleitor);
                    if (jaVotou) {
                        Platform.runLater(() -> showError("❌ Você já votou! Use a opção 'Atualizar Voto' para mudar seu voto."));
                        return;
                    }
                    
                    boolean allowed = votacao.isEleitor(eleitor);
                    if (!allowed) {
                        Platform.runLater(() -> showError("❌ Eleitor não está autorizado."));
                        return;
                    }

                    boolean ok = votacao.setVoto(eleitor, selecionado);
                    if (ok) {
                        Platform.runLater(() -> {
                            showAlert("✅ Sucesso", "🎉 Voto registado com sucesso!\n\nObrigado por participar!");
                            voterName.clear();
                            listView.getSelectionModel().clearSelection();
                            root.setCenter(initialMenu);
                        });
                    } else {
                        Platform.runLater(() -> showError("❌ Falha ao registrar voto. Tente novamente."));
                    }
                } catch (RemoteException e) {
                    Platform.runLater(() -> showError("❌ Erro de comunicação: " + e.getMessage()));
                } catch (Exception e) {
                    Platform.runLater(() -> showError("❌ Erro: " + e.getMessage()));
                }
            }).start();
        });
    }

    private VBox createVoteLoginPane(BorderPane root, VBox votePane, VBox initialMenu, boolean isUpdate) {
        VBox login = new VBox(15);
        login.setAlignment(Pos.CENTER);
        login.setPadding(new Insets(40));
        
        String titleText = isUpdate ? "🔄 Atualizar Voto - Identificação" : "🔐 Identificação do Eleitor";
        String instructionText = isUpdate ? 
            "Identifique-se para atualizar seu voto:" : 
            "Por favor, identifique-se para prosseguir com a votação:";
        
        Label loginTitle = new Label(titleText);
        loginTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
        Label instruction = new Label(instructionText);
        instruction.setStyle("-fx-font-size: 14px;");
        
        TextField idField = new TextField();
        idField.setPromptText("👤 Nome ou Identificador");
        idField.setPrefWidth(350);
        
        Button proceed = new Button("➡️ Prosseguir");
        Button back = new Button("⬅️ Voltar");
        back.setOnAction(ev -> root.setCenter(initialMenu));
        
        HBox buttons = new HBox(10, proceed, back);
        buttons.setAlignment(Pos.CENTER);
        
        proceed.setOnAction(ev -> {
            String id = idField.getText().trim();
            if (id.isEmpty()) {
                showAlert("⚠️ Atenção", "Por favor, informe seu identificador.");
                return;
            }
            
            // prefill voterName field in votePane
            if (votePane.getChildren().size() > 1 && votePane.getChildren().get(1) instanceof TextField) {
                TextField voterNameField = (TextField) votePane.getChildren().get(1);
                voterNameField.setText(id);
            }
            root.setCenter(votePane);
        });
        
        login.getChildren().addAll(loginTitle, instruction, idField, buttons);
        return login;
    }

    private void connectToServer() throws RemoteException, NotBoundException, MalformedURLException {
        Registry registry = LocateRegistry.getRegistry("localhost", 8080);
        try {
            votacao = (Votacao) registry.lookup("localhost/Votacao");
        } catch (NotBoundException | RemoteException e) {
            throw new RemoteException("Não foi possível localizar o serviço 'localhost/Votacao' no registry: " + e.getMessage(), e);
        }
    }

    private void showAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private void showError(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("❌ Erro");
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private boolean confirmDialog(String title, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        return a.showAndWait().filter(b -> b.getButtonData().isDefaultButton()).isPresent();
    }

    @Override
    public void stop() {
        // Cleanup scheduled tasks
        if (scheduleChecker != null) {
            scheduleChecker.cancel(true);
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}