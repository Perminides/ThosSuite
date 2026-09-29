package app.messaging;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import app.messaging.repository.MessageRepository;
import app.shared.Log;
import app.shared.model.ThrowingSupplier;
import app.shared.ui.MessageContactDialog;

/**
 * Die Identitätsregel der Suite: Gehört eine Rohkennung zu einem Menschen, den es schon gibt?
 *
 * <p>Sie steht hier im Kern und nicht im Zweig, weil sie keine Eigenheit von Signal oder WhatsApp
 * ist. Im Gegenteil — ihr Zweck ist gerade, dass derselbe Mensch beide Quellen umspannen kann:
 * {@code msg_contact_mapping} ordnet je Quelle eine Kennung demselben Kontakt zu, und der Dialog
 * fragt vor dem Anlegen, ob es ihn schon gibt. Läge die Regel zweimal im Zweig, änderte man sie
 * an zwei Stellen oder gar nicht.</p>
 *
 * <p>Der Resolver hält zwei Caches, damit nicht jede Nachricht in die Datenbank greift: welche
 * Kennung zu welchem Kontakt gehört, und wer schon als Mitglied eines Chats eingetragen ist.
 * Beide werden beim Bauen gefüllt.</p>
 */
public class ContactResolver {

	private final String quellenId;
	private final String anzeigename;
	private final MessageRepository repo;

	private final Map<String, Integer> contactByRawId = new LinkedHashMap<>();
	private final Map<Integer, Set<Integer>> chatMembers = new LinkedHashMap<>();

	/**
	 * @param quellenId   der Schlüssel in der Datenbank, etwa {@code signal}
	 * @param anzeigename wie die Quelle im Dialog heißt, etwa {@code Signal}
	 */
	public ContactResolver(String quellenId, String anzeigename, MessageRepository repo) {
		this.quellenId = quellenId;
		this.anzeigename = anzeigename;
		this.repo = repo;
		contactByRawId.putAll(repo.loadKnownContacts(quellenId));
		chatMembers.putAll(repo.loadChatMembers());
	}

	/**
	 * Die Kontakt-Id zu einer Rohkennung. Ist sie unbekannt, fragt der Dialog — damit dieselbe
	 * Person nicht ein zweites Mal entsteht, nur weil sie bisher aus der anderen Quelle bekannt war.
	 *
	 * @param vorschlag liefert den Namen, mit dem der Dialog vorbelegt wird, oder {@code null}.
	 *                  Wird <b>nur</b> gefragt, wenn die Kennung unbekannt ist — das Ermitteln kann
	 *                  die Fremd-Datenbank kosten, und bei einem Cache-Treffer braucht es niemand.
	 */
	public int resolve(Connection thos, String rawIdentifier, ThrowingSupplier<String> vorschlag)
			throws Exception {
		Integer bekannt = contactByRawId.get(rawIdentifier);
		if (bekannt != null)
			return bekannt;

		MessageContactDialog.Result result = MessageContactDialog.show(
				anzeigename, rawIdentifier, vorschlag.get(), repo.loadAllContactsByDisplayName());

		if (result == null)
			throw new IllegalStateException("[FAILFAST] " + anzeigename + "-Import abgebrochen: "
					+ "Kein Name im Kontakt-Dialog eingegeben. rawIdentifier=" + rawIdentifier);

		int contactId;
		if (result.existingContactId() != null) {
			contactId = result.existingContactId();
			Log.info(this, "[" + quellenId + "] Kontakt zugeordnet: contactId=" + contactId);
		} else {
			contactId = repo.insertContact(thos, result.newDisplayName());
			Log.info(this, "[" + quellenId + "] Kontakt angelegt: '" + result.newDisplayName() + "'");
		}

		repo.insertContactMapping(thos, quellenId, rawIdentifier, contactId);
		contactByRawId.put(rawIdentifier, contactId);
		return contactId;
	}

	/** Trägt den Kontakt als Mitglied des Chats ein, falls er es nicht schon ist. */
	public void ensureChatMember(Connection thos, int chatId, int contactId) throws SQLException {
		if (!chatMembers.computeIfAbsent(chatId, _ -> new HashSet<>()).add(contactId))
			return;
		repo.insertChatMemberIfAbsent(thos, chatId, contactId);
	}
}
