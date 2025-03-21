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

package esac.archive.esasky.cl.web.client.repository;

import com.allen_sauer.gwt.log.client.Log;
import com.github.nmorel.gwtjackson.client.ObjectMapper;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Timer;
import esac.archive.absi.modules.cl.aladinlite.widget.client.model.CoordinatesObject;
import esac.archive.absi.modules.cl.aladinlite.widget.client.model.SearchArea;
import esac.archive.esasky.cl.web.client.CommonEventBus;
import esac.archive.esasky.cl.web.client.api.model.FootprintListJSONWrapper;
import esac.archive.esasky.cl.web.client.api.model.GeneralSkyObject;
import esac.archive.esasky.cl.web.client.api.model.IJSONWrapper;
import esac.archive.esasky.cl.web.client.api.model.SourceListJSONWrapper;
import esac.archive.esasky.cl.web.client.callback.*;
import esac.archive.esasky.cl.web.client.event.ExtTapFovEvent;
import esac.archive.esasky.cl.web.client.event.ExtTapToggleEvent;
import esac.archive.esasky.cl.web.client.event.TreeMapNewDataEvent;
import esac.archive.esasky.cl.web.client.internationalization.TextMgr;
import esac.archive.esasky.cl.web.client.model.DescriptorCountAdapter;
import esac.archive.esasky.cl.web.client.model.SingleCount;
import esac.archive.esasky.cl.web.client.query.*;
import esac.archive.esasky.cl.web.client.status.CountStatus;
import esac.archive.esasky.cl.web.client.status.DescriptorObserver;
import esac.archive.esasky.cl.web.client.status.GUISessionStatus;
import esac.archive.esasky.cl.web.client.utility.*;
import esac.archive.esasky.cl.web.client.utility.JSONUtils.IJSONRequestCallback;
import esac.archive.esasky.ifcs.model.client.GeneralJavaScriptObject;
import esac.archive.esasky.ifcs.model.coordinatesutils.SkyViewPosition;
import esac.archive.esasky.ifcs.model.descriptor.*;
import esac.archive.esasky.ifcs.model.shared.ESASkySSOSearchResult.ESASkySSOObjType;
import esac.archive.esasky.ifcs.model.shared.EsaSkyConstants;
import esac.archive.esasky.ifcs.model.shared.contentdescriptors.UCD;

import java.util.*;
import java.util.stream.Collectors;

import static esac.archive.esasky.cl.web.client.utility.EsaSkyWebConstants.EXTTAP_FOV_LIMIT;

public class DescriptorRepository {

    public interface CommonTapDescriptorListMapper extends ObjectMapper<CommonTapDescriptorList> {
    }

    public interface TapDescriptorListMapper extends ObjectMapper<TapDescriptorList> {
    }

    public interface SingleCountListMapper extends ObjectMapper<List<SingleCount>> {
    }

    // Key: category
    private final Map<String, DescriptorCountAdapter> descriptorCountAdapterMap = new HashMap<>();

    // Key: category
    private final Map<String, List<DescriptorObserver>> waitingDescriptorObservers = new HashMap<>();

    // Key: tableName
    private final Map<String, List<String>> tableCategoryMap = new HashMap<>();


    private boolean isExtTapOpen = false;

    private ICountRequestHandler countRequestHandler;
    private SearchArea searchArea;
    private static DescriptorRepository instance;


    public static DescriptorRepository getInstance() {
        if (instance == null) {
        	instance = new DescriptorRepository();
        }
        return instance;
    }

    private DescriptorRepository() {
    }

    public void setCountRequestHandler(ICountRequestHandler countRequestHandler) {
        this.countRequestHandler = countRequestHandler;
    }

    public void setDescriptorCountAdapter(String category, DescriptorCountAdapter dca) {
        if (descriptorCountAdapterMap.containsKey(category)) {
            for (CommonTapDescriptor desc : descriptorCountAdapterMap.get(category).getDescriptors()) {
                descriptorCountAdapterMap.get(category).getCountStatus().markForRemoval(desc);
            }
        }

        descriptorCountAdapterMap.put(category, dca);

        for (CommonTapDescriptor desc : dca.getDescriptors()) {
            tableCategoryMap.computeIfAbsent(desc.getTableName(), k -> new LinkedList<>());
            tableCategoryMap.get(desc.getTableName()).add(desc.getCategory());

        }

        if (waitingDescriptorObservers.containsKey(category)) {
            waitingDescriptorObservers.get(category).forEach(DescriptorObserver::onDescriptorLoaded);
            waitingDescriptorObservers.remove(category);
        }
    }

    public void registerDescriptorLoadedObserver(String category, DescriptorObserver observer) {
        if (descriptorCountAdapterMap.containsKey(category)) {
            observer.onDescriptorLoaded();
        } else {
            waitingDescriptorObservers.computeIfAbsent(category, k -> new LinkedList<>());
            waitingDescriptorObservers.get(category).add(observer);
        }
    }

    public boolean addDescriptor(String category, CommonTapDescriptor descriptor) {
        CommonTapDescriptorList descriptorList = descriptorCountAdapterMap.get(category).getTapDescriptorList();
        List<CommonTapDescriptor> descriptors = descriptorList.getDescriptors();
        if (descriptors.stream().noneMatch(d ->
                Objects.equals(d.getMission(), descriptor.getMission())
                        && Objects.equals(d.getTableName(), descriptor.getTableName()))) {
            descriptors.add(descriptor);
            descriptorList.setDescriptors(descriptors);
            DescriptorCountAdapter dca = new DescriptorCountAdapter(descriptorList, category, null);
            setDescriptorCountAdapter(category, dca);

            return true;
        }

        return false;
    }

    public DescriptorCountAdapter getDescriptorCountAdapter(String category) {
        return descriptorCountAdapterMap.get(category);
    }

    public CommonTapDescriptorList getDescriptorList(String category) {
        DescriptorCountAdapter dca = getDescriptorCountAdapter(category);
        return dca != null ? dca.getTapDescriptorList() : null;
    }

    public List<CommonTapDescriptor> getDescriptors(String category) {
        CommonTapDescriptorList tdl = getDescriptorList(category);
        return tdl != null ? tdl.getDescriptors() : null;
    }

    public boolean hasDescriptors(String category) {
        List<CommonTapDescriptor> descriptors = getDescriptors(category);
        return descriptors != null && !descriptors.isEmpty();
    }

    public boolean hasAllDescriptors(String... categories) {
        return categories != null && Arrays.stream(categories).allMatch(this::hasDescriptors);
    }

    public CommonTapDescriptor getFirstDescriptor(String category) {
        List<CommonTapDescriptor> descriptors = getDescriptors(category);

        if (descriptors != null && !descriptors.isEmpty()) {
            return descriptors.get(0);
        } else {
            return null;
        }
    }

    public CommonTapDescriptor getFirstDescriptor(String category, String mission) {
        List<CommonTapDescriptor> descriptors = getDescriptors(category);

        if (descriptors != null && !descriptors.isEmpty()) {
            return descriptors.stream().filter(d -> d.getMission().equals(mission)).findFirst().orElse(null);
        } else {
            return null;
        }
    }

    public CommonTapDescriptor createExternalDescriptor(List<TapMetadataDescriptor> metadataDescriptorList, String tapUrl,
                                                        String tableName, String missionName, String description, String query,
                                                        boolean useFovLimiter, boolean useUnprocessedQuery) {
        String name = useUnprocessedQuery
                ? missionName + " (" + query + ")"
                : missionName + " (" + tableName + ")";

        CommonTapDescriptor commonTapDescriptor = new CommonTapDescriptor();
        commonTapDescriptor.setMetadata(metadataDescriptorList);
        commonTapDescriptor.setCategory(EsaSkyWebConstants.CATEGORY_EXTERNAL);
        commonTapDescriptor.setSchemaName(EsaSkyWebConstants.SCHEMA_EXTERNAL);
        commonTapDescriptor.setLongName(name);
        commonTapDescriptor.setShortName(missionName);
        commonTapDescriptor.setMission(missionName);
        commonTapDescriptor.setTapUrl(tapUrl);
        commonTapDescriptor.setIsExternal(true);
        commonTapDescriptor.setTableName(tableName);
        commonTapDescriptor.setFovLimit(10.0);
        commonTapDescriptor.setDescription(description);
        commonTapDescriptor.setCustom(true);
        commonTapDescriptor.setBulkDownloadUrl(EsaSkyWebConstants.DATA_REQUEST_URL);

        boolean hasPointColumns = commonTapDescriptor.getRaColumn() != null && commonTapDescriptor.getDecColumn() != null;
        boolean hasRegionColumn = commonTapDescriptor.getRegionColumn() != null;
        commonTapDescriptor.setFovLimitDisabled(!useFovLimiter || (!hasPointColumns && !hasRegionColumn));
        commonTapDescriptor.setUseIntersectsPolygon(!hasPointColumns && hasRegionColumn);

        commonTapDescriptor.setGroupColumn1("dataproduct_type");
        commonTapDescriptor.setGroupColumn2("facility_name");

        if (useUnprocessedQuery) {
            commonTapDescriptor.setUnprocessedADQL(query);
        } else {
            final String from = "FROM";
            final String where = "WHERE";

            // Make sure "from" is uppercase
            int start = query.toUpperCase().indexOf(from);
            if (start >= 0) {
                String subStr = query.substring(start, start + from.length());
                query = query.replace(subStr, from);
            }

            // Make sure "where" is uppercase
            start = query.toUpperCase().indexOf(where);
            if (start >= 0) {
                String subStr = query.substring(start, start + where.length());
                query = query.replace(subStr, where);
            }


            String[] whereSplit = query.split(where);
            if (whereSplit.length > 1) {
                commonTapDescriptor.setWhereADQL(whereSplit[1]);
            }
            String[] fromSplit = query.split(from);
            commonTapDescriptor.setSelectADQL(fromSplit[0]);
        }

        return commonTapDescriptor;

    }


    public void addExternalDataCenterDescriptor(CommonTapDescriptor descriptor) {
        if (addDescriptor(EsaSkyWebConstants.CATEGORY_EXTERNAL, descriptor)) {
            updateCount4ExtTap(descriptor, TextMgr.getInstance().getText("treemap_zero_count_alert").replace("$MISSION$", descriptor.getMission()));
        }
    }

    public void resetExternalDataCenterDescriptors() {
        DescriptorCountAdapter oldDca = descriptorCountAdapterMap.get(EsaSkyWebConstants.CATEGORY_EXTERNAL);
        CommonTapDescriptorList descriptorList = oldDca.getTapDescriptorList();
        descriptorList.getDescriptors().stream().filter(TapDescriptorBase::isCustom).forEach(y -> oldDca.getCountStatus().markForRemoval(y));


        List<CommonTapDescriptor> descriptors = descriptorList.getDescriptors().stream().filter(x -> !x.isCustom() && x.getParent() == null).collect(Collectors.toList());
        descriptorList.setDescriptors(descriptors);
        DescriptorCountAdapter dca = new DescriptorCountAdapter(descriptorList, EsaSkyWebConstants.CATEGORY_EXTERNAL, null);
        setDescriptorCountAdapter(EsaSkyWebConstants.CATEGORY_EXTERNAL, dca);
        CommonEventBus.getEventBus().fireEvent(new TreeMapNewDataEvent(null, true, EsaSkyWebConstants.CATEGORY_EXTERNAL));
    }


    public List<TapMetadataDescriptor> mockSpatialMetadata(String raColumn, String decColumn, String regionColumn) {
        List<TapMetadataDescriptor> result = new LinkedList<>();

        if (raColumn != null && !raColumn.isEmpty()) {
            TapMetadataDescriptor raMeta = new TapMetadataDescriptor();
            raMeta.setName(raColumn);
            raMeta.setUcd(UCD.POS_EQ_RA.getValue());
            result.add(raMeta);
        }


        if (decColumn != null && !decColumn.isEmpty()) {
            TapMetadataDescriptor decMeta = new TapMetadataDescriptor();
            decMeta.setName(decColumn);
            decMeta.setUcd(UCD.POS_EQ_DEC.getValue());
            result.add(decMeta);

        }

        if (regionColumn != null && !regionColumn.isEmpty()) {
            TapMetadataDescriptor regionMeta = new TapMetadataDescriptor();
            regionMeta.setName(regionColumn);
            regionMeta.setUcd(UCD.POS_OUTLINE.getValue() + ";" + UCD.OBS_FIELD.getValue());
            result.add(regionMeta);
        }

        return result;
    }

    public CommonTapDescriptor createCustomExternalTapDescriptor(String name, String tapUrl, boolean dataOnlyInView, String adql, String bulkDownloadUrl, String bulkDownloadIdColumn) {
        CommonTapDescriptor descriptor = new CommonTapDescriptor();

        descriptor.setShortName(name);
        descriptor.setLongName(name);
        descriptor.setMission(name);
        descriptor.setCredits(name);
        descriptor.setFovLimit(180.0);
        descriptor.setShapeLimit(3000);
        descriptor.setUseIntersectsPolygon(true);
        descriptor.setTapUrl(tapUrl);
        descriptor.setCustom(true);
        descriptor.setIsExternal(true);
        descriptor.setCategory(EsaSkyWebConstants.CATEGORY_EXTERNAL);
        descriptor.setDescription(" ");
        descriptor.setBulkDownloadUrl(bulkDownloadUrl);
        descriptor.setBulkDownloadIdColumn(bulkDownloadIdColumn);

        descriptor.setMetadata(mockSpatialMetadata(EsaSkyWebConstants.S_RA, EsaSkyWebConstants.S_DEC, EsaSkyWebConstants.S_REGION));

        if (!dataOnlyInView) {
            descriptor.setFovLimitDisabled(true);
        }
        final String from = "FROM";
        final String where = "WHERE";
        final String orderBy = "ORDER BY";

        // Make sure "from" is uppercase

        int start = adql.toUpperCase().indexOf(from);
        if (start >= 0) {
            String subStr = adql.substring(start, start + from.length());
            adql = adql.replace(subStr, from);
        }

        // Make sure "where" is uppercase
        start = adql.toUpperCase().indexOf(where);
        if (start >= 0) {
            String subStr = adql.substring(start, start + where.length());
            adql = adql.replace(subStr, where);
        }


        // Make sure "order by" is uppercase
        start = adql.toUpperCase().indexOf(orderBy);
        if (start >= 0) {
            String subStr = adql.substring(start, start + orderBy.length());
            adql = adql.replace(subStr, orderBy);
        }


        String[] whereSplit = adql.split(where);
        if (whereSplit.length > 1) {
            descriptor.setWhereADQL(whereSplit[1]);
        }else{
            String[] orderBySplit = adql.split(orderBy);
            if (orderBySplit.length > 1) {
                descriptor.setOrderByADQL(orderBySplit[1]);
            }
        }

        String[] fromSplit = adql.split(from);
        descriptor.setSelectADQL(fromSplit[0]);

        String[] tapTable = fromSplit[1].split("\\s");
        descriptor.setTableName(tapTable[1]);
        return descriptor;
    }

    public void initDescriptors(List<String> schemas, String category, Promise<CommonTapDescriptorList> promise) {
        Log.debug("[DescriptorRepository] Into DescriptorRepository.initDescriptors");

        if (!GUISessionStatus.getIsInScienceMode()) {
            GUISessionStatus.setDoCountOnEnteringScienceMode();
        }

        TAPDescriptorService.getInstance().fetchDescriptors(schemas, category, new IJSONRequestCallback() {

            @Override
            public void onSuccess(String responseText) {
                CommonTapDescriptorListMapper mapper = GWT.create(CommonTapDescriptorListMapper.class);
                CommonTapDescriptorList mappedDescriptorList = mapper.read(responseText);
                WavelengthUtils.setWavelengthRangeMaxMin(mappedDescriptorList.getDescriptors());

                // If external we don't have any column metadata, we need to fetch it.
                boolean anyExternal = mappedDescriptorList.getDescriptors().stream().anyMatch(CommonTapDescriptor::isExternal);
                if (anyExternal) {
                    initializeColumns(mappedDescriptorList, promise);
                } else {
                    promise.fulfill(mappedDescriptorList);
                }
            }

            @Override
            public void onError(String errorCause) {
                Log.error("[DescriptorRepository] initDescriptors ERROR: " + errorCause);
                final Timer retryTimer = new Timer() {
                    @Override
                    public void run() {
                        if (!promise.isCompleted()) {
                            initDescriptors(schemas, category, promise);
                        }
                    }
                };

                retryTimer.schedule(500);

            }
        });

    }

    private void initializeColumns(CommonTapDescriptorList mappedDescriptorList, Promise<CommonTapDescriptorList> promise) {
        final int[] descriptorsInitialized = {0};
        for (CommonTapDescriptor commonTapDescriptor : mappedDescriptorList.getDescriptors()) {
            if (commonTapDescriptor.isExternal()) {
                TAPDescriptorService.getInstance().initializeColumns(commonTapDescriptor, new IJSONRequestCallback() {
                    @Override
                    public void onSuccess(String responseText) {
                        TapDescriptorListMapper mapper = GWT.create(TapDescriptorListMapper.class);
                        TapDescriptorList mappedDescriptorList2 = mapper.read(responseText);

                        boolean isSchemaQuery = commonTapDescriptor.getCategory().equals(EsaSkyWebConstants.CATEGORY_PUBLICATIONS);
                        commonTapDescriptor.setMetadata(ExtTapUtils.getMetadataFromTapDescriptorList(mappedDescriptorList2, isSchemaQuery));

                    }

                    @Override
                    public void onError(String errorCause) {
                        Log.error("[DescriptorRepository] initializeColumns ERROR fetching external metadata: " + errorCause);
                    }

                    @Override
                    public void whenComplete() {
                        descriptorsInitialized[0]++;
                        if (descriptorsInitialized[0] == mappedDescriptorList.getTotal()) {
                            promise.fulfill(mappedDescriptorList);
                        }
                    }
                });
            } else {
                descriptorsInitialized[0]++;
            }
        }
    }

    public void doCountAll() {
        // Single dynamic count
        requestSingleCount();
        if (isExtTapOpen) {
            updateCount4AllExtTaps();
        }
    }

    public void updateCount4AllExtTaps() {
        double fov = CoordinateUtils.getCenterCoordinateInJ2000().getFov();
        CommonEventBus.getEventBus().fireEvent(new ExtTapFovEvent(fov));
        if (!tapAreaTooLargeForExternal(fov, searchArea)) {
            for (CommonTapDescriptor descriptor : getDescriptors(EsaSkyWebConstants.CATEGORY_EXTERNAL)) {
                if (EsaSkyConstants.TREEMAP_LEVEL_SERVICE == descriptor.getLevel()) {
                    if (!getDescriptorCountAdapter(EsaSkyWebConstants.CATEGORY_EXTERNAL).getCountStatus().countStillValid(descriptor)) {
                        updateCount4ExtTap(descriptor, null);
                    }
                }
            }
        }
    }

    public static boolean tapAreaTooLargeForExternal(double fov, SearchArea searchArea) {
        return tapAreaTooLargeForExternal(fov, searchArea, EXTTAP_FOV_LIMIT);
    }

    public static boolean tapAreaTooLargeForExternal(double fov, SearchArea searchArea, double fovLimit) {
        if (searchArea == null) {
            return fov > fovLimit;
        }
        if (searchArea.isCircle()) {
            return Float.parseFloat(searchArea.getRadius()) * 2 > fovLimit;
        }
        CoordinatesObject[] points = searchArea.getJ2000Coordinates();
        if (points.length >= 3) {
            double minRa = points[0].getRaDeg();
            double maxRa = points[0].getRaDeg();
            double minDec = points[0].getDecDeg();
            double maxDec = points[0].getDecDeg();
            for (CoordinatesObject point : searchArea.getJ2000Coordinates()) {
                minRa = Math.min(minRa, point.getRaDeg());
                maxRa = Math.max(maxRa, point.getRaDeg());
                minDec = Math.min(minDec, point.getDecDeg());
                maxDec = Math.max(maxDec, point.getDecDeg());
            }
            if (minRa == maxRa || minDec == maxDec) {
                return false;
            }
            double areaApproximation = (maxRa - minRa) * (maxDec - minDec);
            return areaApproximation > fovLimit * fovLimit;
        } else {
            // If the polygon doesn't have at least three nodes, fallback to using fov
            return fov > fovLimit;
        }
    }

    public void updateCount4ExtTap(CommonTapDescriptor descriptor, String zeroCountMessage) {
        final CountStatus cs = getDescriptorCountAdapter(EsaSkyWebConstants.CATEGORY_EXTERNAL).getCountStatus();
        if (!cs.containsDescriptor(descriptor)) {
            cs.addDescriptor(descriptor);
        }

        String adql = TAPExtTapService.getInstance().getCountAdql(descriptor, searchArea);

        String url = EsaSkyWebConstants.EXT_TAP_URL + "?"
                + EsaSkyConstants.EXT_TAP_ACTION_FLAG + "=" + EsaSkyConstants.EXT_TAP_ACTION_REQUEST + "&"
                + EsaSkyConstants.EXT_TAP_ADQL_FLAG + "=" + adql + "&"
                + EsaSkyConstants.EXT_TAP_URL_FLAG + "=" + descriptor.getTapUrl();

        JSONUtils.getJSONFromUrl(url, new ExtTapCheckCallback(adql, descriptor, cs,
                countRequestHandler.getProgressIndicatorMessage() + " " + descriptor.getMission(), zeroCountMessage));
    }


    public void doCountSSO(String ssoName, ESASkySSOObjType ssoType) {
        String url = TAPUtils.getTAPQuery(URL.encodeQueryString(TAPSSOService.getInstance().getCount(ssoName, ssoType)),
                EsaSkyConstants.JSON);

        Log.debug("[doCountSSO] SSO count Query [" + url + "]");
        JSONUtils.getJSONFromUrl(url, new SsoCountRequestCallback(
                getDescriptorCountAdapter(EsaSkyWebConstants.CATEGORY_SSO), ssoName, ssoType));
    }

    public void updateSearchArea(SearchArea area) {
        this.searchArea = area;
        for (DescriptorCountAdapter countAdapters : descriptorCountAdapterMap.values()) {
            for (CommonTapDescriptor descriptor : countAdapters.getDescriptors()) {
                descriptor.setSearchArea(area);
            }
        }

        doCountAll();
    }

    public SearchArea getSearchArea() {
        return this.searchArea;
    }

    public boolean hasSearchArea() {
        return this.searchArea != null;
    }

    private static long lastestSingleCountTimecall;

    public void requestSingleCount() {

        final SkyViewPosition skyViewPosition = CoordinateUtils.getCenterCoordinateInJ2000();

        final long timecall = System.currentTimeMillis();
        lastestSingleCountTimecall = timecall;
        String url = "";

        if (searchArea != null) {
            url = TAPUtils.getTAPCountQuery(URL.encodeQueryString(
                    TAPSingleCountService.getInstance().getCountSearchArea(searchArea)));
        } else {
            url = TAPUtils.getTAPCountQuery(URL.encodeQueryString(
                    TAPSingleCountService.getInstance().getCountStcs(AladinLiteWrapper.getAladinLite())));
        }

        CountRequestCallback callback = new CountRequestCallback(countRequestHandler.getProgressIndicatorMessage(), url, new CountRequestCallback.IOnSuccess () {
			
			@Override
			public void onSucess(Response response) {
				try {
	                if (timecall < lastestSingleCountTimecall) {
	                    Log.warn(this.getClass().getSimpleName() + " discarded server answer with timecall=" + timecall
	                            + " , dif:" + (lastestSingleCountTimecall - timecall));
	                    return;
	                }
	                SingleCountListMapper scMapper = GWT.create(SingleCountListMapper.class);
	                List<SingleCount> singleCountList = scMapper.read(response.getText());
	                doUpdateSingleCount(singleCountList, skyViewPosition, searchArea);
	
	
	                } catch (Exception ex) {
	                    Log.error("[DescriptorRepository] requestSingleCount.onSuccess ERROR: " + ex.getMessage(), ex);
	                }
				}
		});
        JSONUtils.getJSONFromUrl(url, callback);
    }


    public CommonTapDescriptor getDescriptorFromMission(String category, String mission) {
        if (category != null) {
            return getDescriptorCountAdapter(category).getDescriptorByMission(mission);
        }

        return null;
    }


    private void doUpdateSingleCount(List<SingleCount> singleCountList, final SkyViewPosition skyViewPosition, SearchArea searchArea) {

        ArrayList<String> remainingDescriptors = new ArrayList<>(tableCategoryMap.keySet());
        List<CommonTapDescriptor> descriptors = new ArrayList<>();

        setCount(singleCountList, skyViewPosition, remainingDescriptors, descriptors, searchArea);

        //Handling that the fast count doesn't give any results for missing missions in the area, so we set them to 0
        setZeroCountOnNoResponseMissions(skyViewPosition, remainingDescriptors, descriptors, searchArea);

        if (!descriptors.isEmpty()) {
            notifyCountChange(descriptors.stream().filter(d -> !d.getCategory().equals(EsaSkyWebConstants.CATEGORY_SSO)).collect(Collectors.toList()));
        }
    }

    private void notifyCountChange(List<CommonTapDescriptor> descriptors) {
        Set<String> categories = descriptors.stream().map(CommonTapDescriptor::getCategory).collect(Collectors.toSet());
        List<DescriptorCountAdapter> descriptorCountAdapterList = descriptorCountAdapterMap.values().stream()
                .filter(dca -> categories.contains(dca.getCategory())).collect(Collectors.toList());

        CommonEventBus.getEventBus().fireEvent(new TreeMapNewDataEvent(descriptorCountAdapterList));

        for (DescriptorCountAdapter dca : descriptorCountAdapterList) {
            dca.getCountStatus().updateCount();
        }
    }

    private void setCount(List<SingleCount> singleCountList, final SkyViewPosition skyViewPosition,
                          ArrayList<String> remainingDescriptors, List<CommonTapDescriptor> descriptors, SearchArea searchArea) {

        for (SingleCount singleCount : singleCountList) {
            List<String> categories = tableCategoryMap.get(singleCount.getTableName());
            if (categories != null) {
                for (String category : categories) {
                    DescriptorCountAdapter descriptorCountAdapter = descriptorCountAdapterMap.get(category);
                    CountStatus cs = descriptorCountAdapter.getCountStatus();
                    CommonTapDescriptor descriptor = descriptorCountAdapter.getDescriptorByTable(singleCount.getTableName());

                    if (descriptor != null) {
                        final int count = Optional.ofNullable(singleCount.getCount()).orElse(0);
                        cs.setCountDetails(descriptor, count, System.currentTimeMillis(), skyViewPosition, searchArea);
                        remainingDescriptors.remove(singleCount.getTableName());
                        descriptors.add(descriptor);
                    }
                }
            } else {
                Log.warn("[DescriptorRepository] doUpdateSingleCount. TABLE_NAME: '" + singleCount.getTableName()
                        + "' NOT FOUND IN DESCRIPTORS!");
            }
        }
    }

    private void setZeroCountOnNoResponseMissions(final SkyViewPosition skyViewPosition, ArrayList<String> remainingDescriptors,
                                                  List<CommonTapDescriptor> descriptors, SearchArea searchArea) {
        for (String tableName : remainingDescriptors) {
            List<String> categories = tableCategoryMap.get(tableName);

            for (String category : categories) {
                DescriptorCountAdapter descriptorCountAdapter = descriptorCountAdapterMap.get(category);

                final int count = 0;
                for (CommonTapDescriptor descriptor : descriptorCountAdapter.getDescriptors()) {
                    if (!descriptor.getCategory().equals(EsaSkyWebConstants.CATEGORY_EXTERNAL) && descriptor.getTableName().equals(tableName)) {
                        CountStatus cs = descriptorCountAdapter.getCountStatus();
                        cs.setCountDetails(descriptor, count, System.currentTimeMillis(), skyViewPosition, searchArea);
                        descriptors.add(descriptor);
                    }
                }
            }
        }
    }

    public CommonTapDescriptor initUserDescriptor(List<TapMetadataDescriptor> metadataList, IJSONWrapper jsonWrapper, GeneralSkyObject generalSkyObject) {
        CommonTapDescriptor commonTapDescriptor = new CommonTapDescriptor();
        commonTapDescriptor.setMetadata(metadataList);

        commonTapDescriptor.setColor(jsonWrapper.getOverlaySet().getColor());
        commonTapDescriptor.setProperties(commonTapDescriptor.getDecColumn(), generalSkyObject.getDec_deg());
        commonTapDescriptor.setProperties(commonTapDescriptor.getRaColumn(), generalSkyObject.getRa_deg());
        commonTapDescriptor.setProperties(commonTapDescriptor.getLongName(), generalSkyObject.getName());
        commonTapDescriptor.setProperties(commonTapDescriptor.getShortName(), generalSkyObject.getName());
        commonTapDescriptor.setProperties(commonTapDescriptor.getIdColumn(), generalSkyObject.getId());

        commonTapDescriptor.setColumns(metadataList);
        commonTapDescriptor.setLongName(jsonWrapper.getOverlaySet().getOverlayName());
        commonTapDescriptor.setShortName(jsonWrapper.getOverlaySet().getOverlayName());
        commonTapDescriptor.setMission(jsonWrapper.getOverlaySet().getOverlayName());
        commonTapDescriptor.setSampEnabled(false);
        commonTapDescriptor.setColor(jsonWrapper.getOverlaySet().getColor());

        if (jsonWrapper instanceof FootprintListJSONWrapper) {
            commonTapDescriptor.setCategory(EsaSkyWebConstants.CATEGORY_OBSERVATIONS);
        } else if (jsonWrapper instanceof SourceListJSONWrapper) {
            commonTapDescriptor.setCategory(EsaSkyWebConstants.CATEGORY_CATALOGUES);
        }

        return commonTapDescriptor;
    }

    public CommonTapDescriptor initUserDescriptor4MOC(String name, GeneralJavaScriptObject options) {
        CommonTapDescriptor descriptor = new CommonTapDescriptor();

        descriptor.setMission(name);
        descriptor.setLongName(name);
        descriptor.setShortName(name);
        descriptor.setSampEnabled(false);
        descriptor.setFovLimit(360.0);
        descriptor.setCategory(EsaSkyWebConstants.CATEGORY_EXTERNAL);

        return descriptor;
    }


    public void registerExtTapObserver() {

        CommonEventBus.getEventBus().addHandler(ExtTapToggleEvent.TYPE,
                event -> {
                    boolean wasOpen = isExtTapOpen;
                    isExtTapOpen = event.isOpen();
                    if (!wasOpen && isExtTapOpen) {
                        updateCount4AllExtTaps();
                    }
                });
    }

    public void setIsExtTapOpen(boolean isExtTapOpen) {
        this.isExtTapOpen = isExtTapOpen;
    }
}