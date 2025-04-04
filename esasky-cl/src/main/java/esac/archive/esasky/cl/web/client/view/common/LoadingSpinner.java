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

package esac.archive.esasky.cl.web.client.view.common;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.FlowPanel;

public class LoadingSpinner extends FlowPanel{
	private static Resources resources = GWT.create(Resources.class);
	private CssResource style;

	public interface Resources extends ClientBundle {

		@Source("loadingSpinner.css")
		@CssResource.NotStrict
		CssResource style();
	}
	
	public LoadingSpinner(boolean large) {
		super();
		style = resources.style();
		style.ensureInjected();
		if(large) {
			getElement().setInnerHTML("<div class=\"esasky__spinner__large\"><div></div><div></div><div></div><div></div><div></div></div>");
		} else {
			getElement().setInnerHTML("<div class=\"esasky__spinner\"><div></div><div></div><div></div><div></div><div></div></div>");
		}
	}
	
	public static String getLoadingSpinner() {
	    return new LoadingSpinner(true).getElement().getString();
	}
}

