package org.safehaus.kiskis.mgmt.ui.hadoop;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Runo;
import org.safehaus.kiskis.mgmt.ui.hadoop.wizard.Wizard;

/**
 * Created by daralbaev on 08.04.14.
 */
public class HadoopForm extends CustomComponent {
    private final Wizard wizard;

    public HadoopForm() {
        setSizeFull();

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSpacing(true);
        verticalLayout.setSizeFull();

        TabSheet sheet = new TabSheet();
        sheet.setStyleName(Runo.TABSHEET_SMALL);
        sheet.setSizeFull();

        wizard = new Wizard();
        sheet.addTab(wizard.getContent(), "Install");


        verticalLayout.addComponent(sheet);
        setCompositionRoot(verticalLayout);
    }
}
