/*
ESASky
Copyright (C) 2025 European Space Agency

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package esac.archive.esasky.cl.web.client.view.ctrltoolbar;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import esac.archive.absi.modules.cl.aladinlite.widget.client.AladinLiteConstants;
import esac.archive.absi.modules.cl.aladinlite.widget.client.event.AladinLiteCoordinatesOrFoVChangedEvent;
import esac.archive.absi.modules.cl.aladinlite.widget.client.event.AladinLiteCoordinatesOrFoVChangedEventHandler;
import esac.archive.esasky.cl.web.client.CommonEventBus;
import esac.archive.esasky.cl.web.client.Modules;
import esac.archive.esasky.cl.web.client.callback.ICommand;
import esac.archive.esasky.cl.web.client.callback.Promise;
import esac.archive.esasky.cl.web.client.event.*;
import esac.archive.esasky.cl.web.client.internationalization.TextMgr;
import esac.archive.esasky.cl.web.client.model.DescriptorCountAdapter;
import esac.archive.esasky.cl.web.client.model.entities.EntityContext;
import esac.archive.esasky.cl.web.client.presenter.CtrlToolBarPresenter;
import esac.archive.esasky.cl.web.client.presenter.MainPresenter;
import esac.archive.esasky.cl.web.client.presenter.PublicationPanelPresenter;
import esac.archive.esasky.cl.web.client.presenter.SelectSkyPanelPresenter.View;
import esac.archive.esasky.cl.web.client.status.GUISessionStatus;
import esac.archive.esasky.cl.web.client.utility.*;
import esac.archive.esasky.cl.web.client.utility.JSONUtils.IJSONRequestCallback;
import esac.archive.esasky.cl.web.client.view.MainLayoutPanel;
import esac.archive.esasky.cl.web.client.view.common.buttons.EsaSkyButton;
import esac.archive.esasky.cl.web.client.view.common.buttons.EsaSkyToggleButton;
import esac.archive.esasky.cl.web.client.view.common.icons.Icons;
import esac.archive.esasky.cl.web.client.view.ctrltoolbar.planningmenu.PlanObservationPanel;
import esac.archive.esasky.cl.web.client.view.ctrltoolbar.publication.PublicationPanel;
import esac.archive.esasky.cl.web.client.view.ctrltoolbar.selectsky.SelectSkyPanel;
import esac.archive.esasky.cl.web.client.view.ctrltoolbar.treemap.TreeMapContainer;
import esac.archive.esasky.ifcs.model.descriptor.*;
import esac.archive.esasky.ifcs.model.shared.ESASkyTarget;
import esac.archive.esasky.ifcs.model.shared.EsaSkyConstants;

import java.util.*;

/**
 * @author ESDC team Copyright (c) 2015- European Space Agency
 */
public class CtrlToolBar extends Composite implements CtrlToolBarPresenter.View {

    private FlowPanel ctrlToolBarPanel;
    private SelectSkyPanel selectSkyPanel;
    private PublicationPanel publicationPanel;
    private PlanObservationPanel planObservationPanel;
    private GwPanel gwPanel;
    private OutreachHstPanel outreachHstImagePanel;
    private OutreachJwstPanel outreachJwstPanel;
    private  OutreachEuclidPanel outreachEuclidPanel;

    private String HiPSFromURL = null;
    private String unwantedRandomTargets = "";
    private final TreeMapContainer observationTreeMapContainer = new TreeMapContainer(EntityContext.ASTRO_IMAGING);
    private final TreeMapContainer catalogTreeMapContainer = new TreeMapContainer(EntityContext.ASTRO_CATALOGUE);
    private final TreeMapContainer spectraTreeMapContainer = new TreeMapContainer(EntityContext.ASTRO_SPECTRA);
    private final TreeMapContainer ssoTreeMapContainer = new TreeMapContainer(EntityContext.SSO);
    private final ExtTapPanel extTapPanel = new ExtTapPanel();
    enum TabIndex {TREEMAP, REGISTRY, VIZIER, ESA}
    private HashMap<String, CustomTreeMap> customTreeMaps = new HashMap<String, CustomTreeMap>();
    private Timer labelRotationTimer;
    private EsaSkyButton exploreBtn;
    private EsaSkyToggleButton selectSkyButton;
    private EsaSkyToggleButton planObservationButton;
    private BadgeButton observationButton;
    private BadgeButton catalogButton;
    private BadgeButton spectraButton;
    private BadgeButton ssoButton;
    private EsaSkyToggleButton extTapButton;
    private EsaSkyToggleButton gwButton;
    private EsaSkyToggleButton outreachHstButton;

    private EsaSkyToggleButton outreachJwstButton;

    private EsaSkyToggleButton outreachEuclidButton;
    private EsaSkyToggleButton publicationsButton;
    private EsaSkyToggleButton targetListButton;

    private final int suggestedPositionLeft = 5;
    private final int suggestedPositionTop = 77;

    private final CssResource style;
    private Resources resources = GWT.create(Resources.class);

    private HandlerRegistration latestHandler;

    public static interface Resources extends ClientBundle {

        @Source("ctrlToolBar.css")
        @CssResource.NotStrict
        CssResource style();

    }

    public CtrlToolBar(String hips) {
        this.HiPSFromURL = hips;

        this.style = resources.style();
        this.style.ensureInjected();

        initView();

        CommonEventBus.getEventBus().addHandler(TargetDescriptionEvent.TYPE, event -> addTargetBox(event.getTargetName(), event.getTargetDescription(), event.isRightSide()));

        CommonEventBus.getEventBus().addHandler(ShowImageListEvent.TYPE, event -> {
            switch (event.sender) {
                case HST:
                    CtrlToolBar.this.outreachHstImagePanel.toggle();
                    closeAllOtherPanels(outreachHstButton);
                    break;
                case JWST:
                    CtrlToolBar.this.outreachJwstPanel.toggle();
                    closeAllOtherPanels(outreachJwstButton);
                    break;
                case EUCLID:
                    CtrlToolBar.this.outreachEuclidPanel.toggle();
                    closeAllOtherPanels(outreachEuclidButton);
                    break;
            }

        });

        CommonEventBus.getEventBus().addHandler(CloseImageListEvent.TYPE, event -> {
            switch (event.sender) {
                case HST:
                    CtrlToolBar.this.outreachHstImagePanel.close();
                    closeAllOtherPanels(outreachHstButton);
                    break;
                case JWST:
                    CtrlToolBar.this.outreachJwstPanel.close();
                    closeAllOtherPanels(outreachJwstButton);
                    break;
                case EUCLID:
                    CtrlToolBar.this.outreachEuclidPanel.close();
                    closeAllOtherPanels(outreachEuclidButton);
                    break;
                default:
                    CtrlToolBar.this.outreachEuclidPanel.close();
                    CtrlToolBar.this.outreachJwstPanel.close();
                    CtrlToolBar.this.outreachHstImagePanel.close();
            }

        });

        MainLayoutPanel.addMainAreaResizeHandler(event -> setTargetDialogSuggestedPosition(false));
    }

    private void initView() {

        ctrlToolBarPanel = new FlowPanel();

        selectSkyPanel = SelectSkyPanel.init(this.HiPSFromURL);
        selectSkyPanel.setSuggestedPosition(suggestedPositionLeft, suggestedPositionTop);
        selectSkyPanel.definePositionFromTopAndLeft();
        selectSkyPanel.hide();
        EsaSkyButton skiesBtn = createSkiesMenuBtn();
        ctrlToolBarPanel.add(skiesBtn);
        MainLayoutPanel.addElementToMainArea(selectSkyPanel);

        ctrlToolBarPanel.add(createObservationBtn());
        MainLayoutPanel.addElementToMainArea(observationTreeMapContainer);
        observationTreeMapContainer.setSuggestedPosition(suggestedPositionLeft, suggestedPositionTop);
        observationTreeMapContainer.definePositionFromTopAndLeft();
        observationTreeMapContainer.registerObserver(() -> observationButton.setToggleStatus(false));

        ctrlToolBarPanel.add(createCatalogBtn());
        MainLayoutPanel.addElementToMainArea(catalogTreeMapContainer);
        catalogTreeMapContainer.setSuggestedPosition(suggestedPositionLeft, suggestedPositionTop);
        catalogTreeMapContainer.definePositionFromTopAndLeft();
        catalogTreeMapContainer.registerObserver(() -> catalogButton.setToggleStatus(false));

        ctrlToolBarPanel.add(createSpectraBtn());
        MainLayoutPanel.addElementToMainArea(spectraTreeMapContainer);
        spectraTreeMapContainer.setSuggestedPosition(suggestedPositionLeft, suggestedPositionTop);
        spectraTreeMapContainer.definePositionFromTopAndLeft();
        spectraTreeMapContainer.registerObserver(() -> spectraButton.setToggleStatus(false));

        ctrlToolBarPanel.add(createExtTapBtn());
        MainLayoutPanel.addElementToMainArea(extTapPanel);
        extTapPanel.setSuggestedPosition(suggestedPositionLeft, suggestedPositionTop);
        extTapPanel.definePositionFromTopAndLeft();
        extTapPanel.registerObserver(() -> {
            extTapButton.setToggleStatus(false);
            CommonEventBus.getEventBus().fireEvent(new ExtTapToggleEvent(false));
        });
        if (!Modules.getModule(EsaSkyWebConstants.MODULE_EXTTAP)) {
            hideWidget(extTapButton);
        }

        ctrlToolBarPanel.add(createSsoBtn());
        MainLayoutPanel.addElementToMainArea(ssoTreeMapContainer);
        ssoTreeMapContainer.setSuggestedPosition(suggestedPositionLeft, suggestedPositionTop);
        ssoTreeMapContainer.definePositionFromTopAndLeft();
        ssoTreeMapContainer.registerObserver(() -> ssoButton.setToggleStatus(false));

        gwButton = createGwBtn();

        ctrlToolBarPanel.add(gwButton);
        MainLayoutPanel.addElementToMainArea(gwPanel);
        if (!Modules.getModule(EsaSkyWebConstants.MODULE_GW)) {
            hideWidget(gwButton);
        }

        publicationPanel = new PublicationPanel();
        publicationPanel.hide();
        publicationPanel.setSuggestedPosition(suggestedPositionLeft, suggestedPositionTop);
        publicationPanel.definePositionFromTopAndLeft();
        MainLayoutPanel.addElementToMainArea(publicationPanel);

        ctrlToolBarPanel.add(createPublicationsBtn());

        planObservationPanel = PlanObservationPanel.getInstance();
        planObservationPanel.setSuggestedPosition(suggestedPositionLeft, suggestedPositionTop);
        planObservationPanel.definePositionFromTopAndLeft();
        MainLayoutPanel.addElementToMainArea(planObservationPanel);

        ctrlToolBarPanel.add(createPlanObservationBtn());

        exploreBtn = createExploreButton();
        ctrlToolBarPanel.add(exploreBtn);

        targetListButton = createTargetListButton();
        ctrlToolBarPanel.add(targetListButton);

        outreachHstButton = createHstOutreachButton();
        ctrlToolBarPanel.add(outreachHstButton);
        MainLayoutPanel.addElementToMainArea(outreachHstImagePanel);
        if (!Modules.getModule(EsaSkyWebConstants.MODULE_OUTREACH_IMAGE)) {
            hideWidget(outreachHstButton);
        }

        outreachJwstButton = createJwstOutreachButton();
        ctrlToolBarPanel.add(outreachJwstButton);
        MainLayoutPanel.addElementToMainArea(outreachJwstPanel);
        if (!Modules.getModule(EsaSkyWebConstants.MODULE_OUTREACH_JWST)) {
            hideWidget(outreachJwstButton);
        }

        outreachEuclidButton = createEuclidOutreachButton();
        ctrlToolBarPanel.add(outreachEuclidButton);
        MainLayoutPanel.addElementToMainArea(outreachEuclidPanel);
        if (!Modules.getModule(EsaSkyWebConstants.MODULE_OUTREACH_EUCLID)) {
            hideWidget(outreachEuclidButton);
        }

        if (Modules.getModule(EsaSkyWebConstants.MODULE_KIOSK_BUTTONS)) {
            rotateButtonLabelVisibility(Arrays.asList(skiesBtn, exploreBtn, targetListButton, outreachHstButton, outreachJwstButton, outreachEuclidButton));
        }

        initWidget(ctrlToolBarPanel);

        updateModuleVisibility();
    }

    @Override
    protected void onLoad() {
        MainPresenter.getInstance().getTargetPresenter().getTargetListPanel().addCloseHandler(event -> {
            rotateButtonLabelVisibility(Arrays.asList(selectSkyButton, exploreBtn, targetListButton, outreachHstButton, outreachJwstButton, outreachEuclidButton));
            targetListButton.setToggleStatus(false);
        });
        checkPanelsVisibility();
    }

    private void checkPanelsVisibility() {
        if (selectSkyPanel.isShowing() || outreachHstImagePanel.isShowing() || outreachJwstPanel.isShowing() || MainPresenter.getInstance().getTargetPresenter().getTargetListPanel().isShowing()) {
            hideAllLabels(Arrays.asList(selectSkyButton, exploreBtn, targetListButton, outreachHstButton, outreachJwstButton, outreachEuclidButton));
        }
    }

    private EsaSkyButton createSkiesMenuBtn() {
        boolean isKiosk = Modules.getModule(EsaSkyWebConstants.MODULE_KIOSK_BUTTONS);
        String label = isKiosk ? TextMgr.getInstance().getText("webConstants_changeSky") : null;

        selectSkyButton = new EsaSkyToggleButton(Icons.getSelectSkyIcon(), label);
        addCommonButtonStyle(selectSkyButton, TextMgr.getInstance().getText("webConstants_manageSkies"));

        selectSkyButton.addClickHandler(event -> {
            CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(selectSkyButton));
            sendGAEvent(GoogleAnalytics.ACT_CTRLTOOLBAR_SKIES);
            hideAllLabels(Arrays.asList(selectSkyButton, exploreBtn, targetListButton, outreachHstButton, outreachJwstButton, outreachEuclidButton));
        });

        selectSkyPanel.addCloseHandler(event -> {
            selectSkyButton.setToggleStatus(false);
            rotateButtonLabelVisibility(Arrays.asList(selectSkyButton, exploreBtn, targetListButton, outreachHstButton, outreachJwstButton, outreachEuclidButton));
        });


        if (isKiosk) {
            selectSkyPanel.setSuggestedPosition(suggestedPositionLeft, 100);
            selectSkyPanel.definePositionFromTopAndLeft();
            addKioskButtonStyle(selectSkyButton);
        }

        return selectSkyButton;
    }

    private EsaSkyToggleButton createPlanObservationBtn() {
        planObservationButton = new EsaSkyToggleButton(Icons.getPlanObservationIcon());
        addCommonButtonStyle(planObservationButton, TextMgr.getInstance().getText("webConstants_projectFutureObservations"));
        planObservationButton.addClickHandler(
                event -> {
                    CtrlToolBar.this.planObservationPanel.toggle();
                    CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(planObservationButton));
                    sendGAEvent(GoogleAnalytics.ACT_CTRLTOOLBAR_PLANNINGTOOL);
                });

        planObservationPanel.addCloseHandler(event -> planObservationButton.setToggleStatus(false));

        return planObservationButton;
    }

    private BadgeButton createObservationBtn() {
        observationButton = createDataPanelBtn(Icons.getObservationIcon(),
                TextMgr.getInstance().getText("webConstants_exploreImageObservations"), EntityContext.ASTRO_IMAGING, observationTreeMapContainer);
        return observationButton;
    }

    private EsaSkyToggleButton createExtTapBtn() {

        extTapButton = new EsaSkyToggleButton(Icons.getExtTapIcon());
        addCommonButtonStyle(extTapButton, TextMgr.getInstance().getText("webConstants_exploreExtTaps"));
        extTapButton.addClickHandler(
                event -> {
                    extTapPanel.toggle();
                    CommonEventBus.getEventBus().fireEvent(new ExtTapToggleEvent(extTapPanel.isShowing()));
                    CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(extTapButton));
                    sendGAEvent(EntityContext.EXT_TAP.toString());
                });
        return extTapButton;
    }

    private EsaSkyToggleButton createGwBtn() {
        gwButton = new EsaSkyToggleButton(Icons.getGwIcon());
        addCommonButtonStyle(gwButton, TextMgr.getInstance().getText("ctrlToolBar_gwTooltip"));
        gwButton.addClickHandler(event -> {
            CtrlToolBar.this.gwPanel.toggle();
            CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(gwButton));
            sendGAEvent(GoogleAnalytics.ACT_CTRLTOOLBAR_GW);
        });

        gwPanel = new GwPanel();
        gwPanel.setSuggestedPosition(suggestedPositionLeft, suggestedPositionTop);
        gwPanel.definePositionFromTopAndLeft();
        gwPanel.hide();
        gwPanel.addCloseHandler(event -> gwButton.setToggleStatus(false));
        return gwButton;
    }

    private BadgeButton createCatalogBtn() {
        catalogButton = createDataPanelBtn(Icons.getCatalogIcon(),
                TextMgr.getInstance().getText("webConstants_exploreCatalogue"), EntityContext.ASTRO_CATALOGUE, catalogTreeMapContainer);
        return catalogButton;
    }

    private BadgeButton createSpectraBtn() {
        spectraButton = createDataPanelBtn(Icons.getSpectraIcon(),
                TextMgr.getInstance().getText("webConstants_exploreSpectral"), EntityContext.ASTRO_SPECTRA, spectraTreeMapContainer);
        return spectraButton;
    }

    private BadgeButton createSsoBtn() {
        ssoButton = createDataPanelBtn(Icons.getSsoIcon(),
                TextMgr.getInstance().getText("webConstants_exploreData"), EntityContext.SSO, ssoTreeMapContainer);
        ssoButton.setDisabledTooltip(TextMgr.getInstance().getText("webConstants_trackSSO"));
        hideWidget(ssoButton);
        return ssoButton;
    }

    private EsaSkyToggleButton createPublicationsBtn() {
        publicationsButton = new EsaSkyToggleButton(Icons.getPublicationsIcon());
        addCommonButtonStyle(publicationsButton, TextMgr.getInstance().getText("webConstants_explorePublications"));

        publicationPanel.addCloseHandler(event -> publicationsButton.setToggleStatus(false));
        return publicationsButton;
    }

    private BadgeButton createDataPanelBtn(ImageResource imageResource, String tooltip, final EntityContext context, final TreeMapContainer treeMapContainer) {
        final EsaSkyToggleButton toggleButton = new EsaSkyToggleButton(imageResource);
        addCommonButtonStyle(toggleButton, tooltip);
        if (context == EntityContext.ASTRO_CATALOGUE) {
            toggleButton.addStyleName("catalogButton");
        }
        final BadgeButton badgeButton = new BadgeButton(toggleButton);
        toggleButton.addClickHandler(
                event -> {
                    treeMapContainer.toggle();
                    CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(badgeButton));
                    sendGAEvent(context.toString());
                });

        return badgeButton;
    }

    private void addCommonButtonStyle(EsaSkyButton button, String tooltip) {
        button.setNonTransparentBackground();
        button.setBigStyle();
        button.addStyleName("ctrlToolBarBtn");
        button.setTitle(tooltip);
    }

    private void addKioskButtonStyle(EsaSkyButton button) {
        button.setVeryBigStyle();
    }

    public EsaSkyButton addCustomButton(ImageResource icon, String iconText, String description) {
        EsaSkyButton button;
        if (icon != null) {
            button = new EsaSkyButton(icon);
        } else {
            button = new EsaSkyButton(iconText);
        }
        addCommonButtonStyle(button, description);
        ctrlToolBarPanel.add(button);
        return button;
    }

    public void removeCustomButton(EsaSkyButton button) {
        ctrlToolBarPanel.remove(button);
    }

    @Override
    public void updateObservationCount(long newCount) {
        observationButton.updateCount(newCount);

    }

    @Override
    public void updateCatalogCount(long newCount) {
        catalogButton.updateCount(newCount);

    }

    @Override
    public void updateSpectraCount(long newCount) {
        spectraButton.updateCount(newCount);

    }

    @Override
    public void updateSsoCount(long newCount) {
        ssoButton.updateCount(newCount);

    }

    @Override
    public void onIsTrackingSSOEventChanged() {
        if (GUISessionStatus.getIsTrackingSSO()) {
            showWidget(ssoButton);
            ssoButton.setTargetName(GUISessionStatus.getTrackedSso().name);
            ssoButton.setToggleStatus(true);

            ssoTreeMapContainer.show();
            sendGAEvent(EntityContext.SSO.toString());
            CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(ssoButton));
        } else {
            hideWidget(ssoButton);
            ssoTreeMapContainer.hide();
            sendGAEvent(EntityContext.SSO.toString());
        }
    }

    @Override
    public void closeTreeMap() {
        observationTreeMapContainer.hide();
        catalogTreeMapContainer.hide();
        spectraTreeMapContainer.hide();
        ssoTreeMapContainer.hide();
    }

    @Override
    public void closeAllPanels() {
        closeAllOtherPanels(null);
    }

    @Override
    public void closeAllOtherPanels(Widget button) {
        if (!observationButton.equals(button)) {
            observationTreeMapContainer.hide();
        }
        if (!catalogButton.equals(button)) {
            catalogTreeMapContainer.hide();
        }
        if (!spectraButton.equals(button)) {
            spectraTreeMapContainer.hide();
        }
        if (!ssoButton.equals(button)) {
            ssoTreeMapContainer.hide();
        }
        if (!selectSkyButton.equals(button)) {
            selectSkyPanel.hide();
        }
        if (!planObservationButton.equals(button)) {
            planObservationPanel.hide();
        }
        if (!publicationsButton.equals(button)) {
            publicationPanel.hide();
        }
        if (!extTapButton.equals(button)) {
            extTapPanel.hide();
        }
        if (!gwButton.equals(button)) {
            gwPanel.hide();
        }
        if (!outreachHstButton.equals(button)) {
            outreachHstImagePanel.hide();
        }
        if (!outreachJwstButton.equals(button)) {
            outreachJwstPanel.hide();
        }

        if (!outreachEuclidButton.equals(button)) {
            outreachEuclidPanel.hide();
        }

        for (CustomTreeMap customTreeMap : customTreeMaps.values()) {
            if (!customTreeMap.button.equals(button)) {
                customTreeMap.treeMapContainer.hide();
            }
        }

    }

    public void updateModuleVisibility() {
        if (Modules.getModule(EsaSkyWebConstants.MODULE_SKIESMENU)) {
            showWidget(selectSkyButton);
        } else {
            hideWidget(selectSkyButton);
        }

        setScienceModeVisibility(GUISessionStatus.getIsInScienceMode());
    }

    private void setScienceModeVisibility(boolean isInScienceMode) {
        if (!isInScienceMode) {
            hideWidget(observationButton);
            hideWidget(catalogButton);
            hideWidget(spectraButton);
            hideWidget(publicationsButton);
            hideWidget(extTapButton);
            hideWidget(gwButton);
            hideWidget(planObservationButton);
        } else {
            showScienceModeWidgets();
        }

        if (Modules.getModule(EsaSkyWebConstants.MODULE_SSO) && GUISessionStatus.getIsTrackingSSO()) {
            showWidget(ssoButton);
        } else {
            hideWidget(ssoButton);
        }

        if (Modules.getModule(EsaSkyWebConstants.MODULE_DICE) && !isInScienceMode) {
            showWidget(exploreBtn);
        } else {
            hideWidget(exploreBtn);
        }
        showOrHideWidget(outreachHstButton, Modules.getModule(EsaSkyWebConstants.MODULE_OUTREACH_IMAGE) && !isInScienceMode);
        showOrHideWidget(outreachJwstButton, Modules.getModule(EsaSkyWebConstants.MODULE_OUTREACH_JWST) && !isInScienceMode);

        // (ESASKYPCR-2504) Show always EUCLID outreach when layout is EUCLID
        showOrHideWidget(outreachEuclidButton,
                Modules.getModule(EsaSkyWebConstants.MODULE_OUTREACH_EUCLID) && (!isInScienceMode || this.isEuclidLayout()));

        showOrHideWidget(targetListButton, Modules.getModule(EsaSkyWebConstants.MODULE_TARGETLIST) && !isInScienceMode);
    }

    /**
     * Verifies if EUCLID layout is being used, so its outreach mode is always shown
     * @return boolean
     */
    private boolean isEuclidLayout() {
        String layout = Window.Location.getParameter(EsaSkyWebConstants.URL_PARAM_LAYOUT);
        return layout != null && layout.equals((EsaSkyConstants.EUCLID_MISSION));
    }

    private void showScienceModeWidgets() {
        showOrHideWidget(observationButton, Modules.getModule(EsaSkyWebConstants.MODULE_OBS));
        showOrHideWidget(catalogButton, Modules.getModule(EsaSkyWebConstants.MODULE_CAT));
        showOrHideWidget(extTapButton, Modules.getModule(EsaSkyWebConstants.MODULE_EXTTAP));
        showOrHideWidget(spectraButton, Modules.getModule(EsaSkyWebConstants.MODULE_SPE));
        showOrHideWidget(publicationsButton, Modules.getModule(EsaSkyWebConstants.MODULE_PUBLICATIONS));
        showOrHideWidget(extTapButton, Modules.getModule(EsaSkyWebConstants.MODULE_EXTTAP));
        showOrHideWidget(ssoButton, Modules.getModule(EsaSkyWebConstants.MODULE_SSO) && GUISessionStatus.getIsTrackingSSO());
        showOrHideWidget(gwButton, Modules.getModule(EsaSkyWebConstants.MODULE_GW));
        showOrHideWidget(planObservationButton, Modules.getModule(EsaSkyWebConstants.MODULE_JWST_PLANNING));
    }

    private void showOrHideWidget(Widget widget, boolean condition) {
        if (condition) {
            showWidget(widget);
        } else {
            hideWidget(widget);
        }
    }

    private void hideWidget(Widget widget) {
        if (widget != null) {
            widget.getElement().getStyle().setDisplay(Display.NONE);
        }
    }

    private void showWidget(Widget widget) {
        if (widget != null) {
            widget.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
        }
    }


    public EsaSkyButton createExploreButton() {
        boolean isKiosk = Modules.getModule(EsaSkyWebConstants.MODULE_KIOSK_BUTTONS);
        String label = isKiosk ? TextMgr.getInstance().getText("webConstants_exploreRandomTarget") : null;

        final EsaSkyButton button = new EsaSkyButton(Icons.getExploreIcon(), label);
        button.getElement().setId("exploreButton");
        addCommonButtonStyle(button, TextMgr.getInstance().getText("webConstants_exploreRandomTarget"));
        button.addClickHandler(event -> {
            if (!exploreActionInProgress) {
                hideAllLabels(Arrays.asList(selectSkyButton, exploreBtn, targetListButton, outreachHstButton, outreachJwstButton, outreachEuclidButton));
                CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(button));
                showRandomSource();
                sendGAEvent(GoogleAnalytics.ACT_CTRLTOOLBAR_DICE);
            }
        });

        if (isKiosk) {
            addKioskButtonStyle(button);
        }

        return button;
    }

    public EsaSkyToggleButton createTargetListButton() {
        boolean isKiosk = Modules.getModule(EsaSkyWebConstants.MODULE_KIOSK_BUTTONS);
        String label = isKiosk ? TextMgr.getInstance().getText("webConstants_exploreTargetList") : null;

        final EsaSkyToggleButton button = new EsaSkyToggleButton(Icons.getTargetListIcon(), label);
        button.getElement().setId("targetListButton");
        addCommonButtonStyle(button, TextMgr.getInstance().getText("webConstants_uploadTargetList"));
        button.addClickHandler(event -> {
            if(!MainPresenter.getInstance().getTargetPresenter().getTargetListPanel().isShowing()){
                CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(targetListButton));
                MainPresenter.getInstance().getTargetPresenter().toggleTargetList();
                if (isKiosk) {
                    MainPresenter.getInstance().getTargetPresenter().getTargetListPanel().setSuggestedPosition(suggestedPositionLeft, 100);
                }
                hideAllLabels(Arrays.asList(selectSkyButton, exploreBtn, targetListButton, outreachHstButton, outreachJwstButton, outreachEuclidButton));
                GoogleAnalytics.sendEvent(GoogleAnalytics.CAT_CTRLTOOLBAR, GoogleAnalytics.ACT_CTRLTOOLBAR_TARGETLIST, "");
            }
            else{
                targetListButton.setToggleStatus(false);
                MainPresenter.getInstance().getTargetPresenter().toggleTargetList();
                rotateButtonLabelVisibility(Arrays.asList(selectSkyButton, exploreBtn, targetListButton, outreachHstButton, outreachJwstButton, outreachEuclidButton));
            }
        });

        if (isKiosk) {
            addKioskButtonStyle(button);
        }

        return button;
    }

    public EsaSkyToggleButton createHstOutreachButton() {
        boolean isKiosk = Modules.getModule(EsaSkyWebConstants.MODULE_KIOSK_BUTTONS);
        String label = isKiosk ? TextMgr.getInstance().getText("webConstants_exploreHstImages") : null;

        outreachHstButton = new EsaSkyToggleButton(Icons.getHubbleIcon(),  label);
        outreachHstButton.getElement().setId("imageButton");
        addCommonButtonStyle(outreachHstButton, TextMgr.getInstance().getText("webConstants_exploreHstImages"));
        outreachHstButton.addClickHandler(event -> {
                    CommonEventBus.getEventBus().fireEvent(new ShowImageListEvent(ShowImageListEvent.Sender.HST));
                    CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(outreachHstButton));
                    if(outreachHstButton.getToggleStatus()){
                        hideAllLabels(Arrays.asList(selectSkyButton, exploreBtn, targetListButton, outreachHstButton, outreachJwstButton, outreachEuclidButton));
                    }
                    sendGAEvent(GoogleAnalytics.ACT_CTRLTOOLBAR_OUTREACH_IMAGE);
                }
        );
        outreachHstImagePanel = new OutreachHstPanel();
        outreachHstImagePanel.setSuggestedPosition(suggestedPositionLeft, suggestedPositionTop);
        outreachHstImagePanel.definePositionFromTopAndLeft();
        outreachHstImagePanel.hide();
        outreachHstImagePanel.addCloseHandler(event -> {
            outreachHstButton.setToggleStatus(false);
            rotateButtonLabelVisibility(Arrays.asList(selectSkyButton, exploreBtn, targetListButton, outreachHstButton, outreachJwstButton, outreachEuclidButton));
        });

        if (isKiosk) {
            outreachHstImagePanel.setSuggestedPosition(suggestedPositionLeft, 100);
            outreachHstImagePanel.definePositionFromTopAndLeft();
            addKioskButtonStyle(outreachHstButton);
        }

        return outreachHstButton;

    }

    public EsaSkyToggleButton createJwstOutreachButton() {
        boolean isKiosk = Modules.getModule(EsaSkyWebConstants.MODULE_KIOSK_BUTTONS);
        String label = isKiosk ? TextMgr.getInstance().getText("webConstants_exploreJwstImages") : null;

        outreachJwstButton = new EsaSkyToggleButton(Icons.getJwstIcon(), label);
        outreachJwstButton.getElement().setId("jwstButton");
        addCommonButtonStyle(outreachJwstButton, TextMgr.getInstance().getText("webConstants_exploreJwstImages"));
        outreachJwstButton.addClickHandler(event -> {
                    CommonEventBus.getEventBus().fireEvent(new ShowImageListEvent(ShowImageListEvent.Sender.JWST));
                    CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(outreachJwstButton));
                    if(outreachJwstButton.getToggleStatus()){
                        hideAllLabels(Arrays.asList(selectSkyButton, exploreBtn, targetListButton, outreachHstButton, outreachJwstButton, outreachEuclidButton));
                    }
                    sendGAEvent(GoogleAnalytics.ACT_CTRLTOOLBAR_OUTREACH_IMAGE);
                }
        );
        outreachJwstPanel = new OutreachJwstPanel();
        outreachJwstPanel.setSuggestedPosition(suggestedPositionLeft, suggestedPositionTop);
        outreachJwstPanel.definePositionFromTopAndLeft();
        outreachJwstPanel.hide();
        outreachJwstPanel.addCloseHandler(event -> {
            outreachJwstButton.setToggleStatus(false);
            rotateButtonLabelVisibility(Arrays.asList(selectSkyButton, exploreBtn, targetListButton, outreachHstButton, outreachJwstButton, outreachEuclidButton));
        });

        if (isKiosk) {
            outreachJwstPanel.setSuggestedPosition(suggestedPositionLeft, 100);
            outreachJwstPanel.definePositionFromTopAndLeft();
            addKioskButtonStyle(outreachJwstButton);
        }

        return outreachJwstButton;

    }


    public EsaSkyToggleButton createEuclidOutreachButton() {
        boolean isKiosk = Modules.getModule(EsaSkyWebConstants.MODULE_KIOSK_BUTTONS);
        String label = isKiosk ? TextMgr.getInstance().getText("webConstants_exploreEuclidImages") : null;

        outreachEuclidButton = new EsaSkyToggleButton(Icons.getEuclidIcon(), label);
        outreachEuclidButton.getElement().setId("euclidButton");
        addCommonButtonStyle(outreachEuclidButton, TextMgr.getInstance().getText("webConstants_exploreEuclidImages"));
        outreachEuclidButton.addClickHandler(event -> {
                    CommonEventBus.getEventBus().fireEvent(new ShowImageListEvent(ShowImageListEvent.Sender.EUCLID));
                    CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(outreachEuclidButton));
                    if(outreachEuclidButton.getToggleStatus()){
                        hideAllLabels(Arrays.asList(selectSkyButton, exploreBtn, targetListButton, outreachHstButton, outreachJwstButton, outreachEuclidButton));
                    }
                    sendGAEvent(GoogleAnalytics.ACT_CTRLTOOLBAR_OUTREACH_IMAGE);
                }
        );
        outreachEuclidPanel = new OutreachEuclidPanel();
        outreachEuclidPanel.setSuggestedPosition(suggestedPositionLeft, suggestedPositionTop);
        outreachEuclidPanel.definePositionFromTopAndLeft();
        outreachEuclidPanel.hide();
        outreachEuclidPanel.addCloseHandler(event -> {
            outreachEuclidButton.setToggleStatus(false);
            rotateButtonLabelVisibility(Arrays.asList(selectSkyButton, exploreBtn, targetListButton, outreachHstButton, outreachJwstButton, outreachEuclidButton));
        });

        if (isKiosk) {
            outreachEuclidPanel.setSuggestedPosition(suggestedPositionLeft, 100);
            outreachEuclidPanel.definePositionFromTopAndLeft();
            addKioskButtonStyle(outreachEuclidButton);
        }

        return outreachEuclidButton;

    }

    //Workaround for enabling and disabling buttons, which causes incorrect style and click behaviour
    //if the mouse never leaves the button itself. Caused by problems in gwt buttons
    private boolean exploreActionInProgress = false;

    private MessageDialogBox targetDialogBox = new MessageDialogBox(new HTML(), "", "skyObject", "TargetDescriptionDialog");

    private void showRandomSource() {
        exploreActionInProgress = true;

        JSONUtils.getJSONFromUrl(EsaSkyWebConstants.RANDOM_SOURCE_URL + "?lang=" + TextMgr.getInstance().getLangCode()
                + "&UNWANTED=" + unwantedRandomTargets, new IJSONRequestCallback() {

            @Override
            public void onSuccess(String responseText) {

                try {

                    final ESASkyTarget esaSkyTarget = ParseUtils.parseJsonTarget(responseText);

                    if (esaSkyTarget.getName().equals("noMoreTargets")) {
                        CtrlToolBar.this.unwantedRandomTargets = "";
                        exploreActionInProgress = false;
                        CtrlToolBar.this.showRandomSource();
                    } else if (!esaSkyTarget.getTitle().isEmpty()
                            && !esaSkyTarget.getDescription().isEmpty()
                            && !esaSkyTarget.getRa().isEmpty()
                            && !esaSkyTarget.getDec().isEmpty()
                            && !esaSkyTarget.getFovDeg().isEmpty()) {
                        AladinLiteWrapper.getInstance().goToTarget(esaSkyTarget.getRa(), esaSkyTarget.getDec(), Double.parseDouble(esaSkyTarget.getFovDeg()), false, AladinLiteConstants.FRAME_J2000);

                        String surveyName = (!esaSkyTarget.getHipsName().isEmpty()) ? esaSkyTarget.getHipsName() : EsaSkyConstants.ALADIN_DEFAULT_SURVEY_NAME;
                        SelectSkyPanel.setSelectedHipsName(surveyName);
                        addTargetBox(esaSkyTarget.getTitle(), esaSkyTarget.getDescription(), false);

                        String targetName = esaSkyTarget.getName();
                        targetName = targetName.replaceAll("[\\[\\]]", "");

                        CtrlToolBar.this.unwantedRandomTargets += "," + targetName;

                        exploreActionInProgress = false;

                    }
                } catch (Exception ex) {
                    Log.error("[CtrlToolBar] getRandomSource onSuccess ERROR: ", ex);
                }
            }

            @Override
            public void onError(String errorCause) {
                Log.error("[CtrlToolBar] getRandomSource ERROR: " + errorCause);
                exploreActionInProgress = false;
            }

        });
    }

    private void addTargetBox(String targetName, String targetDescription, boolean rightSide) {
        targetDialogBox.updateContent(targetDescription, targetName);
        targetDialogBox.show();
        setTargetDialogSuggestedPosition(rightSide);
        if (latestHandler != null) {
            latestHandler.removeHandler();
        }
        final double ra = AladinLiteWrapper.getCenterRaDeg();
        final double dec = AladinLiteWrapper.getCenterDecDeg();
        final double fov = AladinLiteWrapper.getAladinLite().getFovDeg();

        latestHandler = CommonEventBus.getEventBus().addHandler(AladinLiteCoordinatesOrFoVChangedEvent.TYPE,
                new AladinLiteCoordinatesOrFoVChangedEventHandler() {

                    @Override
                    public void onChangeEvent(final AladinLiteCoordinatesOrFoVChangedEvent clickEvent) {
                        if (CoordinateUtils.isTargetOutOfFocus(ra, dec, fov)) {
                            targetDialogBox.hide();
                            latestHandler.removeHandler();
                        }
                    }

                });
    }

    @Override
    public EsaSkyToggleButton getPublicationButton() {
        return publicationsButton;
    }

    public boolean isExtTapOpen() {
        return extTapPanel.isShowing();
    }

    @Override
    public EsaSkyToggleButton getSkyPanelButton() {
        return selectSkyButton;
    }

    @Override
    public View getSelectSkyView() {
        return selectSkyPanel;
    }

    @Override
    public PublicationPanelPresenter.View getPublicationPanelView() {
        return publicationPanel;
    }

    private void sendGAEvent(String eventAction) {
        GoogleAnalytics.sendEvent(GoogleAnalytics.CAT_CTRLTOOLBAR, eventAction, "");
    }

    @Override
    public void addTreeMapData(Collection<DescriptorCountAdapter> descriptorCounts) {

        for (DescriptorCountAdapter descriptorCount : descriptorCounts) {
            switch (descriptorCount.getCategory()) {
                case EsaSkyWebConstants.CATEGORY_OBSERVATIONS:
                    observationTreeMapContainer.addData(descriptorCount.getDescriptors(), descriptorCount.getCounts());
                    break;
                case EsaSkyWebConstants.CATEGORY_CATALOGUES:
                    catalogTreeMapContainer.addData(descriptorCount.getDescriptors(), descriptorCount.getCounts());
                    break;
                case EsaSkyWebConstants.CATEGORY_SPECTRA:
                    spectraTreeMapContainer.addData(descriptorCount.getDescriptors(), descriptorCount.getCounts());
                    break;
                case EsaSkyWebConstants.CATEGORY_SSO:
                    ssoTreeMapContainer.addData(descriptorCount.getDescriptors(), descriptorCount.getCounts());
                    break;
                case EsaSkyWebConstants.CATEGORY_EXTERNAL:
                    extTapPanel.addTreeMapData(descriptorCount.getDescriptors(), descriptorCount.getCounts());
                    break;
                default:
                    Log.warn("[CtrlToolBar] Unknown category " + descriptorCount.getCategory());
            }
        }

    }

    @Override
    public void clearTreeMapData(String category) {
        switch (category) {
            case EsaSkyWebConstants.CATEGORY_OBSERVATIONS:
                observationTreeMapContainer.clearData();
                break;
            case EsaSkyWebConstants.CATEGORY_CATALOGUES:
                catalogTreeMapContainer.clearData();
                break;
            case EsaSkyWebConstants.CATEGORY_SPECTRA:
                spectraTreeMapContainer.clearData();
                break;
            case EsaSkyWebConstants.CATEGORY_SSO:
                ssoTreeMapContainer.clearData();
                break;
            case EsaSkyWebConstants.CATEGORY_EXTERNAL:
                extTapPanel.clearTreeMapData();
                break;
            default:
                Log.warn("[CtrlToolBar] Unknown category " + category);
        }
    }

    public class CustomTreeMap {
        public final TreeMapContainer treeMapContainer;
        public final EsaSkyToggleButton button;

        public CustomTreeMap(TreeMapContainer treeMapContainer, EsaSkyToggleButton button) {
            this.button = button;
            this.treeMapContainer = treeMapContainer;
        }
    }


    public void updateCustomTreeMap(CustomTreeMapDescriptor treeMapDescriptor) {
        CustomTreeMap customTreeMap = customTreeMaps.get(treeMapDescriptor.getName());
        if (customTreeMap != null) {
            LinkedList<Integer> counts = new LinkedList<>();

            for (int i = 0; i < treeMapDescriptor.getMissionDescriptors().size(); i++) {
                counts.add(1);
            }
            customTreeMap.treeMapContainer.updateData(treeMapDescriptor.getMissionDescriptors(), counts);
        }

    }

    public void addCustomTreeMap(CustomTreeMapDescriptor treeMapDescriptor) {
        TreeMapContainer treeMapContainer = new TreeMapContainer(EntityContext.USER_TREEMAP, false);
        treeMapContainer.setHeaderText(treeMapDescriptor.getIconText());

        EsaSkyToggleButton button = new EsaSkyToggleButton(treeMapDescriptor.getIconText());

        customTreeMaps.put(treeMapDescriptor.getName(), new CustomTreeMap(treeMapContainer, button));

        addCommonButtonStyle(button, treeMapDescriptor.getDescription());
        button.addClickHandler(
                event -> {
                    treeMapContainer.toggle();
                    CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(button));
                    GoogleAnalytics.sendEvent(GoogleAnalytics.CAT_CTRLTOOLBAR, GoogleAnalytics.ACT_CTRLTOOLBAR_PLANNINGTOOL, treeMapDescriptor.getName());
                });
        ctrlToolBarPanel.add(button);
        MainLayoutPanel.addElementToMainArea(treeMapContainer);

        treeMapContainer.setSuggestedPosition(suggestedPositionLeft, suggestedPositionTop);
        treeMapContainer.definePositionFromTopAndLeft();
        treeMapContainer.registerObserver(() -> button.setToggleStatus(false));

        LinkedList<Integer> counts = new LinkedList<>();
        for (int i = 0; i < treeMapDescriptor.getMissionDescriptors().size(); i++) {
            counts.add(1);
        }
        treeMapContainer.addData(treeMapDescriptor.getMissionDescriptors(), counts);

    }

    private void setTargetDialogSuggestedPosition(boolean rightSide) {
        int newLeft = 0;
        int newTop = 30;
        if (MainLayoutPanel.getMainAreaWidth() > 800) {
            if (rightSide) {
                newLeft = 10;
                newTop = 77;
            } else {
                newLeft = MainLayoutPanel.getMainAreaWidth() - targetDialogBox.getOffsetWidth() - 10;
                newTop = 77;
            }

        } else {
            newLeft = (MainLayoutPanel.getMainAreaWidth()) / 2 - targetDialogBox.getOffsetWidth() / 2;
            if (newLeft < 0) {
                newLeft = 0;
            }
        }
        targetDialogBox.setSuggestedPosition(newLeft, newTop);
        if (targetDialogBox.getAbsoluteLeft() < (exploreBtn.getAbsoluteLeft() + exploreBtn.getOffsetWidth() + 5)) {
            targetDialogBox.setSuggestedPosition(targetDialogBox.getAbsoluteLeft() - MainLayoutPanel.getMainAreaAbsoluteLeft(), exploreBtn.getAbsoluteTop() - MainLayoutPanel.getMainAreaAbsoluteTop() + exploreBtn.getOffsetHeight() + 5);
        }
    }

    @Override
    public void openGWPanel(int tabId) {
        if (!gwButton.getToggleStatus()) {
            gwButton.toggle();
            gwPanel.toggle();
            CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(gwButton));
        }
        gwPanel.changeTab(tabId);
    }

    @Override
    public void closeGWPanel() {
        if (gwButton.getToggleStatus()) {
            gwButton.toggle();
            gwPanel.close();
        }
    }

    @Override
    public void minimiseGWPanel() {
        if (gwButton.getToggleStatus()) {
            gwButton.toggle();
            gwPanel.hide();
        }
    }

    @Override
    public void getGWIds(Promise<JSONArray> gwDataPromise) {
        gwPanel.getIds(gwDataPromise);
    }

    @Override
    public void getGWData(String id, Promise<JSONObject> gwDataPromise) {
        gwPanel.getData4Id(id, gwDataPromise);
    }

    @Override
    public void getAllGWData(Promise<JSONObject> gwDataPromise) {
        gwPanel.getAllGWData(gwDataPromise);
    }

    @Override
    public void getNeutrinoEventData(Promise<JSONObject> neutrinoDataPromise) {
        gwPanel.getNeutrinoData(neutrinoDataPromise);
    }

    @Override
    public void showGWEvent(String id, Promise<Boolean> showPromise) {
        gwPanel.showEvent(id, showPromise);
    }

    @Override
    public void showGWEvent(String id) {
        gwPanel.showEvent(id);
    }

    @Override
    public void clickExploreButton() {
        exploreBtn.click();
    }

    @Override
    public void openExtTapPanel() {
        if (!extTapButton.getToggleStatus()) {
            extTapButton.toggle();
            extTapPanel.toggle();
            CommonEventBus.getEventBus().fireEvent(new ExtTapToggleEvent(extTapPanel.isShowing()));
            CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(extTapButton));
            sendGAEvent(EntityContext.EXT_TAP.toString());
        }
    }

    @Override
    public void closeExtTapPanel() {
        if (extTapButton.getToggleStatus()) {
            extTapButton.toggle();
            extTapPanel.toggle();
            CommonEventBus.getEventBus().fireEvent(new ExtTapToggleEvent(extTapPanel.isShowing()));
            CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(extTapButton));
            sendGAEvent(EntityContext.EXT_TAP.toString());
        }
    }

    @Override
    public boolean isExtTapPanelOpen() {
        return extTapPanel.isShowing();
    }

    @Override
    public void openOutreachPanel(String id) {
        if(id == null){
            if (!outreachHstButton.getToggleStatus()) {
                outreachHstButton.toggle();
                outreachHstImagePanel.toggle();
                CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(outreachHstButton));
            }
        }else{
            outreachHstImagePanel.selectShape(id);
            CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(outreachHstButton));
        }
    }

    @Override
    public void closeOutreachPanel() {
        if (outreachHstImagePanel.isShowing()) {
            outreachHstButton.toggle();
            outreachHstImagePanel.toggle();
        }
    }

    @Override
    public void openJwstOutreachPanel(String id) {
        if (!outreachJwstButton.getToggleStatus()) {
            outreachJwstButton.toggle();
            outreachJwstPanel.toggle();
            CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(outreachJwstButton));
        }
    }

    @Override
    public void closeJwstOutreachPanel() {
        if (outreachJwstPanel.isShowing()) {
            outreachJwstButton.toggle();
            outreachJwstPanel.toggle();
        }
    }


    @Override
    public void openEuclidOutreachPanel(String id) {
        if (!outreachEuclidButton.getToggleStatus()) {
            outreachEuclidButton.toggle();
            outreachEuclidPanel.toggle();
            CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(outreachEuclidButton));
        }
    }

    @Override
    public void closeEuclidOutreachPanel() {
        if (outreachEuclidPanel.isShowing()) {
            outreachEuclidButton.toggle();
            outreachEuclidPanel.toggle();
        }
    }

    @Override
    public void openExtTapPanelTab(int index) {
        openExtTapPanel();
        extTapPanel.openTab(index);
    }

    public void closeExtTapPanelTab() {
        extTapPanel.hide();
    }


    private OutreachImagePanel getOutreachImagePanel(String telescope) {
        switch (telescope.toUpperCase()) {
            case EsaSkyConstants.HST_MISSION:
                return outreachHstImagePanel;
            case EsaSkyConstants.JWST_MISSION:
                return outreachJwstPanel;
            case EsaSkyConstants.EUCLID_MISSION:
                return outreachEuclidPanel;
            default:
                return null;
        }
    }

    private EsaSkyToggleButton getOutreachImageButton(String telescope) {
        switch (telescope.toUpperCase()) {
            case EsaSkyConstants.HST_MISSION:
                return outreachHstButton;
            case EsaSkyConstants.JWST_MISSION:
                return outreachJwstButton;
            case EsaSkyConstants.EUCLID_MISSION:
                return outreachEuclidButton;
            default:
                return null;
        }
    }

    @Override
    public JSONArray getOutreachImageIds(ICommand command, String telescope) {
        OutreachImagePanel panel = getOutreachImagePanel(telescope);
        if (panel != null) {
            return panel.getAllImageIds(command);
        } else {
            return new JSONArray();
        }
    }


    @Override
    public JSONArray getOutreachImageNames(ICommand command, String telescope) {
        OutreachImagePanel panel = getOutreachImagePanel(telescope);
        if (panel != null) {
            return panel.getAllImageNames(command);
        } else {
            return new JSONArray();
        }
    }

    @Override
    public void showOutreachImage(String id, String telescope) {
        OutreachImagePanel panel = getOutreachImagePanel(telescope);
        EsaSkyToggleButton button = getOutreachImageButton(telescope);

        if (panel != null) {
            panel.selectShape(id);
            CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(button));
        }
    }

    @Override
    public void showOutreachImageByName(String name, String telescope) {
        OutreachImagePanel panel = getOutreachImagePanel(telescope);
        EsaSkyToggleButton button = getOutreachImageButton(telescope);

        if (panel != null) {
            panel.selectShapeByName(name);
            CommonEventBus.getEventBus().fireEvent(new CloseOtherPanelsEvent(button));
        }
    }

    @Override
    public void showOutreachImageMinimized(String id, String telescope) {
        OutreachImagePanel panel = getOutreachImagePanel(telescope);

        if (panel != null) {
            panel.selectShapeMinimized(id);
        }
    }

    @Override
    public void hideOutreachImage(String telescope) {
        OutreachImagePanel panel = getOutreachImagePanel(telescope);

        if (panel != null) {
            panel.close();
        }
    }

    @Override
    public Map<String, Double[]> getSliderValues() {
        Map<String, Double[]> sliderMap = new HashMap<>();
        sliderMap.put(EntityContext.ASTRO_IMAGING.toString(), observationTreeMapContainer.getSliderValues());
        sliderMap.put(EntityContext.ASTRO_CATALOGUE.toString(), catalogTreeMapContainer.getSliderValues());
        sliderMap.put(EntityContext.ASTRO_SPECTRA.toString(), spectraTreeMapContainer.getSliderValues());
        sliderMap.put(EntityContext.SSO.toString(), ssoTreeMapContainer.getSliderValues());
        sliderMap.put(EntityContext.EXT_TAP.toString(), extTapPanel.getTreeMapSliderValues());

        return sliderMap;
    }

    @Override
    public void setSliderValues(Map<String, Double[]> sliderMap) {
        for (String key : sliderMap.keySet()) {
            Double[] values = sliderMap.get(key);
            if (EntityContext.ASTRO_IMAGING.toString().contentEquals(key)) {
                observationTreeMapContainer.setSliderValues(values[0], values[1]);
            } else if (EntityContext.ASTRO_CATALOGUE.toString().contentEquals(key)) {
                catalogTreeMapContainer.setSliderValues(values[0], values[1]);
            } else if (EntityContext.ASTRO_SPECTRA.toString().contentEquals(key)) {
                spectraTreeMapContainer.setSliderValues(values[0], values[1]);
            } else if (EntityContext.SSO.toString().contentEquals(key)) {
                ssoTreeMapContainer.setSliderValues(values[0], values[1]);
            } else if (EntityContext.EXT_TAP.toString().contentEquals(key)) {
                extTapPanel.setTreeMapSliderValues(values[0], values[1]);
            }
        }

    }

    private void hideAllLabels(List<EsaSkyButton> btnList) {
        for (EsaSkyButton btn : btnList) {
            btn.hideLabel();
        }
    }
    private void rotateButtonLabelVisibility(List<EsaSkyButton> btnList) {
        if (labelRotationTimer != null) {
            labelRotationTimer.cancel();
        }

        if (!btnList.isEmpty()) {
            btnList.get(0).showLabel();
        }
        labelRotationTimer = new Timer() {
            @Override
            public void run() {
                for (int i = 0; i < btnList.size(); i++) {
                    EsaSkyButton currentBtn = btnList.get(i);
                    int nextIndex = i + 1 < btnList.size() ? i + 1 : 0;
                    if (currentBtn.isLabelVisible()) {
                        currentBtn.hideLabel();
                        btnList.get(nextIndex).showLabel();
                        break;
                    }

                }
            }

        };

        labelRotationTimer.scheduleRepeating(5000);
    }
}
