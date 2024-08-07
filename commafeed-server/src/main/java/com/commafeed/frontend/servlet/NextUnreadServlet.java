package com.commafeed.frontend.servlet;

import java.net.URI;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.commafeed.CommaFeedConfiguration;
import com.commafeed.backend.dao.FeedCategoryDAO;
import com.commafeed.backend.dao.FeedEntryStatusDAO;
import com.commafeed.backend.dao.FeedSubscriptionDAO;
import com.commafeed.backend.dao.UnitOfWork;
import com.commafeed.backend.model.FeedCategory;
import com.commafeed.backend.model.FeedEntryStatus;
import com.commafeed.backend.model.FeedSubscription;
import com.commafeed.backend.model.User;
import com.commafeed.backend.model.UserSettings.ReadingOrder;
import com.commafeed.backend.service.FeedEntryService;
import com.commafeed.frontend.resource.CategoryREST;
import com.commafeed.security.AuthenticationContext;
import com.google.common.collect.Iterables;

import jakarta.inject.Singleton;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

@Path("/next")
@RequiredArgsConstructor
@Singleton
public class NextUnreadServlet {

	private final UnitOfWork unitOfWork;
	private final FeedSubscriptionDAO feedSubscriptionDAO;
	private final FeedEntryStatusDAO feedEntryStatusDAO;
	private final FeedCategoryDAO feedCategoryDAO;
	private final FeedEntryService feedEntryService;
	private final CommaFeedConfiguration config;
	private final AuthenticationContext authenticationContext;

	@GET
	public Response get(@QueryParam("category") String categoryId, @QueryParam("order") @DefaultValue("desc") ReadingOrder order) {
		User user = authenticationContext.getCurrentUser();
		if (user == null) {
			return Response.temporaryRedirect(URI.create(config.publicUrl())).build();
		}

		FeedEntryStatus status = unitOfWork.call(() -> {
			FeedEntryStatus s = null;
			if (StringUtils.isBlank(categoryId) || CategoryREST.ALL.equals(categoryId)) {
				List<FeedSubscription> subs = feedSubscriptionDAO.findAll(user);
				List<FeedEntryStatus> statuses = feedEntryStatusDAO.findBySubscriptions(user, subs, true, null, null, 0, 1, order, true,
						null, null, null);
				s = Iterables.getFirst(statuses, null);
			} else {
				FeedCategory category = feedCategoryDAO.findById(user, Long.valueOf(categoryId));
				if (category != null) {
					List<FeedCategory> children = feedCategoryDAO.findAllChildrenCategories(user, category);
					List<FeedSubscription> subscriptions = feedSubscriptionDAO.findByCategories(user, children);
					List<FeedEntryStatus> statuses = feedEntryStatusDAO.findBySubscriptions(user, subscriptions, true, null, null, 0, 1,
							order, true, null, null, null);
					s = Iterables.getFirst(statuses, null);
				}
			}
			if (s != null) {
				feedEntryService.markEntry(user, s.getEntry().getId(), true);
			}
			return s;
		});

		String url = status == null ? config.publicUrl() : status.getEntry().getUrl();
		return Response.temporaryRedirect(URI.create(url)).build();
	}
}
