/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.kiskis.mgmt.server.ui.modules.cassandra.wizzard;

import com.vaadin.terminal.Sizeable;
import com.vaadin.ui.*;

/**
 *
 * @author bahadyr
 */
public class Step8 extends FormLayout {

    public Step8(final CassandraWizard cassandraWizard) {
        setCaption("Configuration Step8");
        setSizeFull();

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSizeFull();
        verticalLayout.setHeight(600, Sizeable.UNITS_PIXELS);
        verticalLayout.setMargin(true);

        GridLayout grid = new GridLayout(6, 10);
        grid.setSpacing(true);
        grid.setSizeFull();

        Panel panel = new Panel();
        Label menu = new Label("Cluster Install Wizard<br>"
                + " 1) Welcome<br>"
                + " 2) List nodes<br>"
                + " 3) Installation<br>"
                + " 4) <font color=\"#f14c1a\"><strong>Configuration</strong></font>");
        menu.setContentMode(Label.CONTENT_XHTML);
        panel.addComponent(menu);

        grid.addComponent(menu, 0, 0, 1, 5);
        grid.setComponentAlignment(panel, Alignment.TOP_CENTER);

        Label label = new Label("<strong>Choose directories</strong>");
        label.setContentMode(Label.CONTENT_XHTML);

        grid.addComponent(label, 2, 0, 5, 0);
        grid.setComponentAlignment(label, Alignment.TOP_CENTER);

        final TextField textFieldDataDir = new TextField("Data Directory:");
        grid.addComponent(textFieldDataDir, 2, 1, 5, 1);
        grid.setComponentAlignment(textFieldDataDir, Alignment.TOP_LEFT);

        final TextField textFieldCommitLogDir = new TextField("Commit Log Directory:");
        grid.addComponent(textFieldCommitLogDir, 2, 2, 5, 2);
        grid.setComponentAlignment(textFieldCommitLogDir, Alignment.TOP_LEFT);

        final TextField textFieldSavedCachesDir = new TextField("Saved Caches Directory:");
        grid.addComponent(textFieldSavedCachesDir, 2, 3, 5, 3);
        grid.setComponentAlignment(textFieldSavedCachesDir, Alignment.TOP_LEFT);

        Button next = new Button("Start");
        next.addListener(new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {
                String dataDir = textFieldDataDir.getValue().toString();
                String commitDir = textFieldCommitLogDir.getValue().toString();
                String cacheDir = textFieldSavedCachesDir.getValue().toString();
                if (dataDir.length() > 0 && commitDir.length() > 0 && cacheDir.length() > 0) {
                    cassandraWizard.getCluster().setDataDir(dataDir);
                    cassandraWizard.getCluster().setCommitLogDir(commitDir);
                    cassandraWizard.getCluster().setSavedCacheDir(cacheDir);
                    cassandraWizard.showNext();
                } else {
                    getWindow().showNotification(
                            "Please fill the form.",
                            Window.Notification.TYPE_TRAY_NOTIFICATION);
                }
            }
        });
        Button back = new Button("Back");
        back.addListener(new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {
                cassandraWizard.showBack();
            }
        });

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.addComponent(back);
        horizontalLayout.addComponent(next);

        verticalLayout.addComponent(grid);
        verticalLayout.addComponent(horizontalLayout);

        addComponent(verticalLayout);
    }

}
