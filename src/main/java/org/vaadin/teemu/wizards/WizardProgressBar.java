package org.vaadin.teemu.wizards;

import java.util.List;

import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.VerticalLayout;

/**
 * Displays a progress bar for a {@link Wizard}.
 */
@SuppressWarnings("serial")
// @StyleSheet("wizard-progress-bar.css")
public class WizardProgressBar extends CustomComponent implements WizardProgressListener {

    private final Wizard wizard;

    private final ProgressBar progressBar = new ProgressBar();

    private final HorizontalLayout stepCaptions = new HorizontalLayout();

    private int activeStepIndex;

    public WizardProgressBar(final Wizard wizard) {
        this.setStyleName("wizard-progress-bar");
        this.wizard = wizard;

        this.stepCaptions.setWidth("100%");
        this.progressBar.setWidth("100%");
        this.progressBar.setHeight("13px");

        VerticalLayout layout = new VerticalLayout();
        layout.setWidth("100%");
        layout.addComponent(this.stepCaptions);
        layout.addComponent(this.progressBar);
        this.setCompositionRoot(layout);
        this.setWidth("100%");
    }

    private void updateProgressBar() {
        int stepCount = this.wizard.getSteps().size();
        float padding = (1.0f / stepCount) / 2;
        float progressValue = padding + this.activeStepIndex / (float) stepCount;
        this.progressBar.setValue(progressValue);
    }

    private void updateStepCaptions() {
        this.stepCaptions.removeAllComponents();
        int index = 1;
        for (WizardStep step : this.wizard.getSteps()) {
            Label label = this.createCaptionLabel(index, step);
            this.stepCaptions.addComponent(label);
            index++;
        }
    }

    private Label createCaptionLabel(final int index, final WizardStep step) {
        Label label = new Label(index + ". " + step.getCaption());
        label.addStyleName("step-caption");

        // Add styles for themeing.
        if (this.wizard.isCompleted(step)) {
            label.addStyleName("completed");
        }
        if (this.wizard.isActive(step)) {
            label.addStyleName("current");
        }
        if (this.wizard.isFirstStep(step)) {
            label.addStyleName("first");
        }
        if (this.wizard.isLastStep(step)) {
            label.addStyleName("last");
        }

        return label;
    }

    private void updateProgressAndCaptions() {
        this.updateProgressBar();
        this.updateStepCaptions();
    }

    @Override
    public void activeStepChanged(final WizardStepActivationEvent event) {
        List<WizardStep> allSteps = this.wizard.getSteps();
        this.activeStepIndex = allSteps.indexOf(event.getActivatedStep());
        this.updateProgressAndCaptions();
    }

    @Override
    public void stepSetChanged(final WizardStepSetChangedEvent event) {
        this.updateProgressAndCaptions();
    }

    @Override
    public void wizardCompleted(final WizardCompletedEvent event) {
        this.progressBar.setValue(1.0f);
        this.updateStepCaptions();
    }

    @Override
    public void wizardCancelled(final WizardCancelledEvent event) {
        // NOP, no need to react to cancellation
    }
}
