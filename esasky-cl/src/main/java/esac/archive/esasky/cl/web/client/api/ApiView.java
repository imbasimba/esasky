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

package esac.archive.esasky.cl.web.client.api;


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;

import com.google.gwt.json.client.JSONString;
import esac.archive.absi.modules.cl.aladinlite.widget.client.AladinLiteConstants;
import esac.archive.esasky.cl.web.client.Controller;
import esac.archive.esasky.cl.web.client.utility.AladinLiteWrapper;
import esac.archive.esasky.cl.web.client.utility.CoordinateUtils;
import esac.archive.esasky.cl.web.client.utility.EsaSkyWebConstants;
import esac.archive.esasky.cl.web.client.utility.GoogleAnalytics;
import esac.archive.esasky.cl.web.client.view.MainLayoutPanel;
import esac.archive.esasky.ifcs.model.coordinatesutils.CoordinatesConversion;
import esac.archive.esasky.ifcs.model.coordinatesutils.SkyViewPosition;

public class ApiView extends ApiBase {


    public ApiView(Controller controller) {
        this.controller = controller;
    }


    public void getCenter(String cooFrame, JavaScriptObject widget) {
        SkyViewPosition skyViewPosition;
        if (cooFrame == null || "".equals(cooFrame)) {
            cooFrame = AladinLiteWrapper.getInstance().getCooFrame();
        }
        if (cooFrame.equalsIgnoreCase(EsaSkyWebConstants.ALADIN_J2000_COOFRAME)) {
            skyViewPosition = CoordinateUtils.getCenterCoordinateInJ2000();
        } else {
            skyViewPosition = CoordinateUtils.getCenterCoordinateInGalactic();
        }

        JSONObject coors = new JSONObject();
        coors.put(ApiConstants.RA, new JSONNumber(skyViewPosition.getCoordinate().getRa()));
        coors.put(ApiConstants.DEC, new JSONNumber(skyViewPosition.getCoordinate().getDec()));
        coors.put(ApiConstants.FOV, new JSONNumber(skyViewPosition.getFov()));

        GoogleAnalytics.sendEventWithURL(googleAnalyticsCat, GoogleAnalytics.ACT_PYESASKY_GETCENTER, "Cooframe: " + cooFrame + " returned: " + coors.toString());
        sendBackValuesToWidget(coors, widget);
    }

    public void goTo(String ra, String dec) {
        GoogleAnalytics.sendEvent(googleAnalyticsCat, GoogleAnalytics.ACT_PYESASKY_GOTORADEC, "ra: " + ra + "dec: " + dec);
        AladinLiteWrapper.getInstance().goToObject(ra + " " + dec, false);
    }

    public void goToWithParams(String ra, String dec, double fovDegrees, boolean showTargetPointer, String cooFrame) {
        AladinLiteWrapper.getInstance().goToTarget(ra, dec, fovDegrees, showTargetPointer, cooFrame);
    }

    public void goToTargetName(String targetName) {
        GoogleAnalytics.sendEvent(googleAnalyticsCat, GoogleAnalytics.ACT_PYESASKY_GOTOTARGETNAME, targetName);
        AladinLiteWrapper.getInstance().goToObject(targetName, false);
    }

    public void goToTargetNameWithFoV(String targetName, double fovDeg) {
        AladinLiteWrapper.getInstance().goToObject(targetName, false);
        AladinLiteWrapper.getAladinLite().setZoom(fovDeg);
    }

    public void setFoV(double fov) {
        GoogleAnalytics.sendEventWithURL(googleAnalyticsCat, GoogleAnalytics.ACT_PYESASKY_SETFOV, Double.toString(fov));
        AladinLiteWrapper.getAladinLite().setZoom(fov);
    }

    public void getFoV(JavaScriptObject widget) {
        SkyViewPosition pos = CoordinateUtils.getCenterCoordinateInJ2000();
        JSONObject obj = new JSONObject();
        obj.put(ApiConstants.FOVRA, new JSONNumber(pos.getFov()));
        obj.put(ApiConstants.FOVDEC, new JSONNumber(pos.getFov() * MainLayoutPanel.getMainAreaHeight() / MainLayoutPanel.getMainAreaWidth()));
        sendBackToWidget(obj, widget);
    }

    public void getFovShape(JavaScriptObject widget) {
        String shape;
        double fovDeg = AladinLiteWrapper.getAladinLite().getFovDeg();
        if (AladinLiteWrapper.isCornersInsideHips()) {
            shape = "POLYGON('ICRS', " + (fovDeg < 1 ?
                    AladinLiteWrapper.getAladinLite().getFovCorners(1).toString() + ")" :
                    AladinLiteWrapper.getAladinLite().getFovCorners(2).toString() + ")");
        } else {
            String cooFrame = AladinLiteWrapper.getAladinLite().getCooFrame();
            if (EsaSkyWebConstants.ALADIN_GALACTIC_COOFRAME.equalsIgnoreCase(cooFrame)) {
                // convert to J2000
                double[] ccInJ2000 = CoordinatesConversion.convertPointGalacticToJ2000(
                        AladinLiteWrapper.getAladinLite().getCenterLongitudeDeg(),
                        AladinLiteWrapper.getAladinLite().getCenterLatitudeDeg());
                shape = "CIRCLE('ICRS', " + ccInJ2000[0] + "," + ccInJ2000[1] + ",90)";
            } else {
                shape = "CIRCLE('ICRS', "
                        + AladinLiteWrapper.getAladinLite().getCenterLongitudeDeg() + ","
                        + AladinLiteWrapper.getAladinLite().getCenterLatitudeDeg() + ",90)";
            }
        }

        JSONObject obj = new JSONObject();
        obj.put("shape", new JSONString(shape));
        sendBackToWidget(obj, widget);
    }

    public void setCoordinateFrame(String cooFrame) {
        if (cooFrame == null || "".equals(cooFrame)) {
            cooFrame = AladinLiteWrapper.getInstance().getCooFrame();
        }

        if (cooFrame.equalsIgnoreCase(EsaSkyWebConstants.ALADIN_J2000_COOFRAME)) {
            AladinLiteWrapper.getInstance().setCooFrame(AladinLiteConstants.CoordinateFrame.J2000);
            controller.getRootPresenter().getHeaderPresenter().getView().selectCoordinateFrame(0);
        } else if (cooFrame.equalsIgnoreCase(EsaSkyWebConstants.ALADIN_GALACTIC_COOFRAME)) {
            AladinLiteWrapper.getInstance().setCooFrame(AladinLiteConstants.CoordinateFrame.GALACTIC);
            controller.getRootPresenter().getHeaderPresenter().getView().selectCoordinateFrame(1);
        }
    }

    public void clickExploreButton() {
        controller.getRootPresenter().getCtrlTBPresenter().clickExploreButton();
    }
}
