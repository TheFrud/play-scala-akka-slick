import javax.inject.Inject

import models.Tables.Persons
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

/**
 * Created by Fredrik on 11-Oct-15.
 */
class ApplicationGlobal@Inject() (lifecycle: ApplicationLifecycle) {
  lifecycle.addStopHook{
    () =>
      Future.successful(Persons.db.close())

  }
}
